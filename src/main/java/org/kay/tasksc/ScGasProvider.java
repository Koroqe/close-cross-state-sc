package org.kay.tasksc;

import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.math.BigInteger;

public class ScGasProvider implements ContractGasProvider {

    public static final BigInteger GAS_PRICE = DefaultGasProvider.GAS_PRICE;
    public static final BigInteger GAS_LIMIT = BigInteger.valueOf(1_300_000);

    @Override
    public BigInteger getGasPrice(String contractFunc) {
        switch (contractFunc) {
            case Sc.FUNC_CLOSECHANNEL: return DefaultGasProvider.GAS_LIMIT;
            case Sc.FUNC_COOPERATIVECLOSE: return GAS_LIMIT;
            case Sc.FUNC_OPENCHANNEL: return GAS_LIMIT;
            case Sc.FUNC_SETTLECHANNEL: return GAS_LIMIT;
            default: throw new NotImplementedException();
        }
    }

    @Override
    public BigInteger getGasPrice() {
        return GAS_PRICE;
    }

    @Override
    public BigInteger getGasLimit(String contractFunc) {
        switch (contractFunc) {
            case Sc.FUNC_CLOSECHANNEL: return GAS_PRICE;
            case Sc.FUNC_COOPERATIVECLOSE: return GAS_PRICE;
            case Sc.FUNC_OPENCHANNEL: return GAS_PRICE;
            case Sc.FUNC_SETTLECHANNEL: return GAS_PRICE;
            default: throw new NotImplementedException();
        }
    }

    @Override
    public BigInteger getGasLimit() {
        return GAS_LIMIT;
    }
}
