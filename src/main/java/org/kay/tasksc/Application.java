package org.kay.tasksc;

import java.io.File;
import java.math.BigInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tuples.generated.Tuple5;


public class Application {

    public static final String TOKEN_ADDRESS = "0x5520770f57e02cca633d6abb2e266b70ce8fd82b";
    public static final String SC_ADDRESS = "0x6232f1613542943224bea102decc502bb5d0fced";

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

        log.info(credentialsAlice.getAddress());
        log.info(credentialsBob.getAddress());

        Sc contract = Sc.load(
                SC_ADDRESS, web3j, credentialsAlice, new ScGasProvider() );
        log.info("Smart contract setup");

        String contractAddress = contract.getContractAddress();
        log.info("View contract at https://rinkeby.etherscan.io/address/" + contractAddress);

        Tuple5 channelData = contract.channels(BigInteger.ONE).send();
        log.info("Channel 1: \\n" + channelData.toString());

        // Open channel
        TransactionReceipt transactionReceipt = contract.openChannel(
                credentialsBob.getAddress(),
                BigInteger.ONE,
                BigInteger.valueOf(0))
                .send();

        log.info(transactionReceipt.toString());

        //validate channel
        
    }
}
