package org.kay.tasksc;

import java.io.File;
import java.math.BigInteger;

import io.reactivex.disposables.Disposable;
import org.kay.tasksc.contracts.Cct;
import org.kay.tasksc.contracts.Sc;
import org.kay.tasksc.contracts.ScGasProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tuples.generated.Tuple5;
import org.web3j.tx.gas.DefaultGasProvider;


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

        Sc scContractAlice = Sc.load(
                SC_ADDRESS, web3j, credentialsAlice, new DefaultGasProvider() );
        Sc scContractBob = Sc.load(
                SC_ADDRESS, web3j, credentialsBob, new DefaultGasProvider() );
        Cct tokenContract = Cct.load(
                TOKEN_ADDRESS, web3j, credentialsAlice, new DefaultGasProvider() );
        log.info("Smart contract setup");

        log.info("View SC contract at https://rinkeby.etherscan.io/address/" + scContractAlice.getContractAddress());

        // Open channel
        Disposable disposable = tokenContract
                .approve(scContractAlice.getContractAddress(), BigInteger.valueOf(1))
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
                                        receipt1 -> log.info(receipt1.toString()),
                                        error -> log.info(error.toString()),
                                        // channel opened
                                        () -> {
                                            // Alice generates receipt
                                        }
                                )
                );

        Tuple5 channelData = scContractAlice.channels(BigInteger.valueOf(0)).send();
        log.info("Channel 1: \\n" + channelData.toString());

        //validate channel

    }
}
