package org.kay.tasksc;

import java.io.File;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.security.SignatureException;

import io.reactivex.disposables.Disposable;
import org.bouncycastle.util.encoders.Hex;
import org.kay.tasksc.channel.CooperativeCloseMessage;
import org.kay.tasksc.channel.Receipt;
import org.kay.tasksc.contracts.Cct;
import org.kay.tasksc.contracts.Sc;
import org.kay.tasksc.contracts.ScGasProvider;
import org.kay.tasksc.sign.Sign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tuples.generated.Tuple5;
import org.web3j.tx.gas.DefaultGasProvider;


public class Application {

    public static final String TOKEN_ADDRESS = "0x49bf27ec8b831846da0362dd2e490b338ff93b95";
    public static final String SC_ADDRESS = "0x9629fd449a7d45cc3fa785e54b56fc91ae6cd6fe";

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws Exception {
        new Application().run();
    }

    private void run() throws Exception {

        Web3j web3j = Web3j.build(new HttpService("https://rinkeby.infura.io/v3/4f4c785a6f6849b09b17008f2828ea75"));
        log.info("Connected to Ethereum client version: " + web3j.web3ClientVersion().send().getWeb3ClientVersion());

        Credentials credentialsAlice =
                WalletUtils.loadCredentials(
                        "1234",
                        new File("test_wallet.json"));

        Credentials credentialsBob =
                WalletUtils.loadCredentials(
                        "1234",
                        new File("test_wallet_2.json"));
        log.info("Credentials loaded");

        log.info("Alice's address: " + credentialsAlice.getAddress());
        log.info("Bob's address: " + credentialsBob.getAddress());

        log.info("Smart contract setup");
        Sc scContractAlice = Sc.load(
                SC_ADDRESS, web3j, credentialsAlice, new DefaultGasProvider() );
        Sc scContractBob = Sc.load(
                SC_ADDRESS, web3j, credentialsBob, new DefaultGasProvider() );
        Cct tokenContract = Cct.load(
                TOKEN_ADDRESS, web3j, credentialsAlice, new DefaultGasProvider() );

        log.info("View SC contract at https://rinkeby.etherscan.io/address/" + scContractAlice.getContractAddress());
        log.info("View Token contract at https://rinkeby.etherscan.io/address/" + tokenContract.getContractAddress());

        // Open channel
        Disposable disposable = tokenContract
                .approve(
                        scContractAlice.getContractAddress(),
                        BigInteger.valueOf(1))
                .flowable()
                .subscribe(
                        receipt -> log.info(receipt.toString()),
                        error -> log.info(error.toString()),
                        // tokens approved
                        () -> scContractAlice.openChannel(
                                credentialsBob.getAddress(),
                                BigInteger.ONE,
                                BigInteger.valueOf(0))
                                .flowable()
                                .subscribe(
                                        receipt1 -> {
                                            //get newly created channel info
                                            int channelID = Integer.parseInt(
                                                    receipt1.getLogs().get(1).getTopics().get(1).substring(2),16);
                                            log.info("Channel " +
                                                    channelID + ": " +
                                                    scContractAlice.channels(BigInteger.valueOf(channelID)).send().toString());

                                            // Alice generates receipt sending 1 to Bob
                                            Receipt receipt = new Receipt(
                                                    channelID,
                                                    1,
                                                    0,
                                                    1);
                                            Sign.SignatureData signature = signReceipt(
                                                    receipt,
                                                    credentialsAlice.getEcKeyPair());

                                            // Bob validates receipt from Alice
                                            boolean isValid = validateReceipt(
                                                    receipt,
                                                    signature,
                                                    credentialsAlice.getAddress(),
                                                    receipt.getChannelID(),
                                                    receipt.getNonce(),
                                                    1,
                                                    1);

                                            log.info(isValid ? "Receipt is valid" : "Receipt is invalid");

                                            //Try to do cooperative close
                                            CooperativeCloseMessage message = new CooperativeCloseMessage(
                                                    channelID,
                                                    1,
                                                    0);

                                            Sign.SignatureData sigAlice = signCooperativeCloseMessage(
                                                    message,
                                                    credentialsAlice.getEcKeyPair());

                                            Sign.SignatureData sigBob = signCooperativeCloseMessage(
                                                    message,
                                                    credentialsBob.getEcKeyPair());

                                            byte[] bytesSigAlice = sigDataToBytes(sigAlice);
                                            byte[] bytesSigBob = sigDataToBytes(sigBob);

                                            scContractBob.cooperativeClose(
                                                    BigInteger.valueOf(message.getChannelID()),
                                                    BigInteger.valueOf(message.getaBalance()),
                                                    BigInteger.valueOf(message.getbBalance()),
                                                    bytesSigAlice,
                                                    bytesSigBob)
                                                    .flowable()
                                                    .subscribe(
                                                            receipt2 -> //closing is successful
                                                                    log.info(receipt2.toString()),
                                                            error -> log.info(error.toString())
                                                    );
                                        },
                                        error -> log.info(error.toString())
                                )
                );
    }

    private Sign.SignatureData signReceipt(Receipt receipt, ECKeyPair keys) {
        byte[] message = ByteBuffer.allocate(128)
                .putInt(receipt.getChannelID())
                .putInt(receipt.getNonce())
                .putInt(receipt.getaBalance())
                .putInt(receipt.getbBalance())
                .array();

        return Sign.signMessage(Hash.sha3(message), keys);
    }

    private Sign.SignatureData signCooperativeCloseMessage(CooperativeCloseMessage message, ECKeyPair keys) {
        byte[] signedMessage = ByteBuffer.allocate(96)
                .put(ByteBuffer.allocate(32).put(new byte[28]).putInt(message.getChannelID()).array())
                .put(ByteBuffer.allocate(32).put(new byte[28]).putInt(message.getaBalance()).array())
                .put(ByteBuffer.allocate(32).put(new byte[28]).putInt(message.getbBalance()).array())
                .array();
        return Sign.signMessage(Hash.sha3(signedMessage), keys);
    }

    private String extractAddressFromReceipt(Receipt receipt,
                                Sign.SignatureData signature) {

        byte[] message = ByteBuffer.allocate(128)
                .putInt(receipt.getChannelID())
                .putInt(receipt.getNonce())
                .putInt(receipt.getaBalance())
                .putInt(receipt.getbBalance())
                .array();
        BigInteger pubk;
        try {
            pubk = Sign.signedMessageToKey(Hash.sha3(message), signature);
        } catch (SignatureException e){
            pubk = BigInteger.ZERO;
        }
        return Keys.getAddress(pubk);
    }

    private boolean validateReceipt(Receipt receipt,
                                   Sign.SignatureData signature,
                                   String reqSigner,
                                   int reqChannelID,
                                   int reqNonce,
                                   int reqReceivedBalance,
                                   int reqChannelBalance) {
        String signer = "0x" + extractAddressFromReceipt(receipt, signature);
        int channelBalance = receipt.getaBalance() + receipt.getbBalance();
        return reqNonce == receipt.getNonce()
                && reqChannelID == receipt.getChannelID()
                && reqReceivedBalance == receipt.getbBalance()
                && reqChannelBalance == channelBalance
                && reqSigner.equals(signer);
    }

    private byte[] sigDataToBytes(Sign.SignatureData data) {
        return ByteBuffer.allocate(65)
                .put(data.getR())
                .put(data.getS())
                .put(data.getV())
                .array();
    }
}
