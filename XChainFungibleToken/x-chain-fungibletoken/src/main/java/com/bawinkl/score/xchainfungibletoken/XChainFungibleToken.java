/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bawinkl.score.xchainfungibletoken;

import score.ByteArrayObjectWriter;
import score.Context;
import score.DictDB;
import score.ArrayDB;
import score.VarDB;
import score.BranchDB;
import score.Address;

import java.util.Map;

import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

import java.math.BigInteger;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.Json;

import com.bawinkl.score.xchainfungibletoken.sdos.*;
import com.iconloop.score.token.irc2.IRC2;

public class XChainFungibleToken implements IRC2 {

    // ================================================
    // Consts
    // ================================================
    protected static final Address ZERO_ADDRESS = new Address(new byte[Address.LENGTH]);

    // ================================================
    // SCORE DB
    // ================================================

    private final VarDB<String> name = Context.newVarDB("token_name", String.class);
    private final VarDB<String> symbol = Context.newVarDB("token_symbol", String.class);
    private final VarDB<BigInteger> decimals = Context.newVarDB("decimals", BigInteger.class);
    private final VarDB<BigInteger> totalSupply = Context.newVarDB("total_supply", BigInteger.class);

    //Balances are now referenced by a string representing the Network Address string in the format: "[NetworkID]/[AddressHash]" eg("0x7.icon/hxc5e0b88cb9092bbd8b004a517996139334752f62")
    private final DictDB<String, BigInteger> balances = Context.newDictDB("balances", BigInteger.class);

    // The networkID for this SCORE
    private final VarDB<String> varNetworkID = Context.newVarDB("network_id", String.class);

    // The XCall contract endpoint on the local network
    private final VarDB<Address> varXCallContract = Context.newVarDB("xcall_contract", Address.class);


    public XChainFungibleToken(String _name, String _symbol, int _decimals) {
        // initialize values only at first deployment
        if (this.name.get() == null) {
            this.name.set(ensureNotEmpty(_name));
            this.symbol.set(ensureNotEmpty(_symbol));

            // decimals must be larger than 0 and less than 21
            Context.require(_decimals >= 0, "decimals needs to be positive");
            Context.require(_decimals <= 21, "decimals needs to be equal or lower than 21");
            this.decimals.set(BigInteger.valueOf(_decimals));
        }
    }

    // ================================================
    // XCall Contract Management
    // ================================================

    /**
     * Get the configured XCall Contract
     */
    @External(readonly = true)
    public Address getXCallContract() {
        return varXCallContract.getOrDefault(ZERO_ADDRESS);
    }

    /**
     * Sets the network ID for this score
     * Required to support all local Network Address references
     * Can only be set by the SCORE owner
     * Must be a contract address
     * 
     * @param _value: the contract address
     */
    @External
    public void setXCallContract(Address _value) {
        onlyOwner();
        Context.require(_value.isContract(), "_value must be a contract address.");
        varXCallContract.set(_value);
    }




    // ================================================
    // Network ID Management
    // ================================================

    /**
     * Get the configured network ID for this SCORE
     */
    @External(readonly = true)
    public String getNetworkID() {
        return varNetworkID.getOrDefault("");
    }

    /**
     * Sets the network ID for this score
     * Required to support all local Network Address references
     * Can only be set by the SCORE owner
     * 
     * @param _value: the network ID value
     */
    @External
    public void setNetworkID(String _value) {
        onlyOwner();
        varNetworkID.set(_value);
    }



    // ================================================
    // IRC-2 Methods
    // NOTE: All of these have been modified to use the NetworkAddress object for db
    // tracking data in place of "Address" however the original IRC-2 interface requirements remain intact.
    // Cross chain enabled methods are preceeded by "x_"
    // ================================================

    @External(readonly=true)
    public String name() {
        return name.get();
    }

    @External(readonly=true)
    public String symbol() {
        return symbol.get();
    }

    @External(readonly=true)
    public BigInteger decimals() {
        return decimals.get();
    }

    @External(readonly=true)
    public BigInteger totalSupply() {
        return totalSupply.getOrDefault(BigInteger.ZERO);
    }

    /**
     * This is the original IRC-31 implementation of balanceOf, which looks up the newly implemented NetworkAddress based on the configured network id and the _owner.
     * Required to match the existing IRC-31 interface requirements
     * @param _owner: the ICON address of the owner
     * @param _id: the token ID
     */
    @External(readonly = true)
    public BigInteger balanceOf(Address _owner) {
        Context.require(!varNetworkID.getOrDefault("").isEmpty(), "The Network ID is not configured for this SCORE");
        NetworkAddress ownerAddress = new NetworkAddress(_owner, varNetworkID.get());
        return _balanceOf(ownerAddress);
    }

    /**
     * A XCall compatible implementation of the balanceOf function
     * Returns the balance of a given address
     * This implementation can accept any address, network address or btp address in string format
     * @param _owner: the targeted address in one of the following formats: icon address, network address ([NetworkID]/[Address]) or btp address ([btp://][NetworkID]/[Address])
     * @param _id: the token ID
     */
    @External(readonly = true)
    public BigInteger x_balanceOf(String _owner) {
        Context.require(!varNetworkID.getOrDefault("").isEmpty(), "The Network ID is not configured for this SCORE");
        NetworkAddress ownerAddress = new NetworkAddress(_owner, varNetworkID.get());
        return _balanceOf(ownerAddress);
    }

    private BigInteger _balanceOf(NetworkAddress _owner) {
        return balances.getOrDefault(_owner.toString(), BigInteger.ZERO);
    }

    private void safeSetBalance(NetworkAddress owner, BigInteger amount)
    {
        balances.set(owner.toString(), amount);
    }

    @External
    public void transfer(Address _to, BigInteger _value, @Optional byte[] _data) {
        Context.require(!varNetworkID.getOrDefault("").isEmpty(), "The Network ID is not configured for this SCORE");
        NetworkAddress fromAddress = new NetworkAddress(Context.getCaller(), varNetworkID.get());  
        NetworkAddress toAddress = new NetworkAddress(_to, varNetworkID.get());
        _transfer(fromAddress, toAddress, _value, _data);
    }

    /**
     * A XCall compatible implementation of the transfer method
     * This implementation can accept any address, network address or btp address in string format for _to
     * Returns the balances of a set of owners and ids
     * @param _owners: an array of addresses in string format, network address format ([NetworkID]/[Address]) or btp address format ([btp://][NetworkID]/[Address])
     * @param _id: an array of token IDs
     */
    @External
    public void x_transfer(String _to, BigInteger _value, @Optional byte[] _data) {
        Context.require(!varNetworkID.getOrDefault("").isEmpty(), "The Network ID is not configured for this SCORE");
        NetworkAddress fromAddress = new NetworkAddress(Context.getCaller(), varNetworkID.get());
        NetworkAddress toAddress = new NetworkAddress(_to, varNetworkID.get());
        _transfer(fromAddress, toAddress, _value, _data);
    }

    private void _transfer(NetworkAddress _from, NetworkAddress _to, BigInteger _value, @Optional byte[] _data)
    {
            // check some basic requirements
            Context.require(_value.compareTo(BigInteger.ZERO) >= 0, "_value needs to be positive");
            Context.require(x_balanceOf(_from.toString()).compareTo(_value) >= 0, "Insufficient balance");

            // adjust the balances
            safeSetBalance(_from, x_balanceOf(_from.toString()).subtract(_value));
            safeSetBalance(_to, x_balanceOf(_to.toString()).add(_value));

            // emit Transfer event first
            byte[] dataBytes = (_data == null) ? new byte[0] : _data;
            XTransfer(_from.toString(), _to.toString(), _value, dataBytes);

            // Try to call tokenFallback
            // this will only work for local network contract addresses, so we wrap it in a
            // try/catch
            try
            {
                Address localNetworkToAddress = Address.fromString(_to.getAddress());
                Address localNetworkFromAddress = Address.fromString(_from.getAddress());
                // if the recipient is SCORE, call 'tokenFallback' to handle further operation
                if (localNetworkToAddress.isContract()) {
                    Context.call(localNetworkToAddress, "tokenFallback", localNetworkFromAddress, _value, dataBytes);
                }
            }
            catch(Exception ex) {}
    }


    /**
     * A basic implementation of an external mint function allowed only by the SCORE owner.
     */
    @External
    public void mint(BigInteger amount)
    {
        onlyOwner();
        _x_mint(Context.getCaller().toString(), amount);
    }

    /**
     * Creates `amount` tokens and assigns them to `owner`, increasing the total supply.
     */
    protected void _mint(Address owner, BigInteger amount) {
        Context.require(!varNetworkID.getOrDefault("").isEmpty(), "The Network ID is not configured for this SCORE");
        Context.require(!ZERO_ADDRESS.equals(owner), "Owner address cannot be zero address");
        Context.require(amount.compareTo(BigInteger.ZERO) >= 0, "amount needs to be positive");

        NetworkAddress ownerAddress = new NetworkAddress(owner, varNetworkID.get());
        totalSupply.set(totalSupply().add(amount));
        safeSetBalance(ownerAddress, x_balanceOf(ownerAddress.toString()).add(amount));
        XTransfer(ZERO_ADDRESS.toString(), ownerAddress.toString(), amount, "mint".getBytes());
    }

    protected void _x_mint(String owner, BigInteger amount)
    {
        Context.require(!varNetworkID.getOrDefault("").isEmpty(), "The Network ID is not configured for this SCORE");
        Context.require(!ZERO_ADDRESS.equals(owner), "Owner address cannot be zero address");
        Context.require(amount.compareTo(BigInteger.ZERO) >= 0, "amount needs to be positive");

        NetworkAddress ownerAddress = new NetworkAddress(owner, varNetworkID.get());
        totalSupply.set(totalSupply().add(amount));
        safeSetBalance(ownerAddress, x_balanceOf(ownerAddress.toString()).add(amount));
        XTransfer(ZERO_ADDRESS.toString(), owner.toString(), amount, "mint".getBytes());
    }

    /**
     * Destroys `amount` tokens from `owner`, reducing the total supply.
     */
    protected void _burn(Address owner, BigInteger amount) {
        Context.require(!varNetworkID.getOrDefault("").isEmpty(), "The Network ID is not configured for this SCORE");
        Context.require(!ZERO_ADDRESS.equals(owner), "Owner address cannot be zero address");
        Context.require(amount.compareTo(BigInteger.ZERO) >= 0, "amount needs to be positive");
        Context.require(balanceOf(owner).compareTo(amount) >= 0, "Insufficient balance");

        NetworkAddress ownerAddress = new NetworkAddress(owner, varNetworkID.get());

        safeSetBalance(ownerAddress, x_balanceOf(ownerAddress.toString()).subtract(amount));
        totalSupply.set(totalSupply().subtract(amount));
        XTransfer(ownerAddress.toString(), ZERO_ADDRESS.toString(), amount, "burn".getBytes());
    }

    /**
     * Destroys `amount` tokens from `owner`, reducing the total supply.
     */
    protected void _x_burn(String owner, BigInteger amount) {
        Context.require(!varNetworkID.getOrDefault("").isEmpty(), "The Network ID is not configured for this SCORE");
        Context.require(!ZERO_ADDRESS.equals(owner), "Owner address cannot be zero address");
        Context.require(amount.compareTo(BigInteger.ZERO) >= 0, "amount needs to be positive");
       
        NetworkAddress ownerAddress = new NetworkAddress(owner, varNetworkID.get());
        Context.require(x_balanceOf(ownerAddress.toString()).compareTo(amount) >= 0, "Insufficient balance");

        safeSetBalance(ownerAddress, x_balanceOf(ownerAddress.toString()).subtract(amount));
        totalSupply.set(totalSupply().subtract(amount));
        XTransfer(ownerAddress.toString(), ZERO_ADDRESS.toString(), amount, "burn".getBytes());
    }

    @EventLog(indexed=3)
    public void Transfer(Address _from, Address _to, BigInteger _value, byte[] _data) {}

    @EventLog(indexed=3)
    public void XTransfer(String _from, String _to, BigInteger _value, byte[] _data) {}



    // ================================================
    // XCall Implementation
    // ================================================

    /*
     * Handles the call message received from the source chain
     * can only be called from the XCall service address
     * 
     * @param _from The exterenal address of the caller on the source chain
     * 
     * @param _data The calldata delivered from the caller in the following JSON
     * format (required values are based on the intended method):
     * {
     * method: "methodName", //required
     * data : {
     * _from: "", // A btp/network address string
     * _to: "", // A btp/network address string
     * _value: "0x1" // BigInteger in Loop value representation
     * _data: "", // an encoded byte array string
     * }
     * }
     */
    @External
    public void handleCallMessage(String _from, byte[] _data) {
        onlyCallService();

        NetworkAddress callerAddress = new NetworkAddress(_from, ""); // We don't need to pass in the network ID because
                                                                     // the _from variable is expected to be in btp
                                                                     // address format.

        String dataString = new String(_data);
        JsonObject requestObject = null;
        JsonObject requestData = null;

        try {
            requestObject = Json.parse(dataString).asObject();
            requestData = requestObject.get("data").asObject();
        } catch (Exception ex) {
            Context.revert("_data does not appear to be in the expected JSON format error:" + ex.getMessage());
        }

        if (requestObject == null)
            Context.revert("_data does not appear to be in an expected JSON format or is empty.");

        if (requestData == null)
            Context.revert("_data does not contain required data token in a json format or it is empty.");

        String method = requestObject.get("method").asString();
        Context.require(method.length() > 0, "method token cannot be empty in _data");
     
        if (method.equals("transfer")) {
            _handleTransferMessage(callerAddress, requestData);
        } 
         else {
            Context.revert("Method '" + method + "' is not supported");
        }
    }

    private void _handleTransferMessage(NetworkAddress fromAddress, JsonObject requestData)
    {
        // _transfer(NetworkAddress _from, NetworkAddress _to, BigInteger _value, @Optional byte[] _data)
        Context.require(requestData.contains("_to"), "_to token missing in data for method transferFrom");
        Context.require(requestData.contains("_value"), "_values token missing in data for method transferFrom");

        NetworkAddress toAddress = new NetworkAddress(requestData.get("_to").asString(), "");
        BigInteger value = new BigInteger(requestData.get("_value").asString().replace("0x", ""), 16);

        byte[] dataBytes =  new byte[] {};

        if (requestData.contains("_data"))
            dataBytes = requestData.get("_data").asString().getBytes();

        _transfer(fromAddress, toAddress, value, dataBytes);
    }

    // ================================================
    // Utility Methods
    // ================================================

    private String ensureNotEmpty(String str) {
        Context.require(str != null && !str.trim().isEmpty(), "str is null or empty");
        assert str != null;
        return str.trim();
    }
    
    /**
     * When called, will only allow score owner to submit the transaction
     */
    private void onlyOwner() {
        Context.require(Context.getCaller().equals(Context.getOwner()), "Caller is not the SCORE owner.");
    }

    /**
     * When called, only allows the method to be called by the configured xcall
     * service contract
     */
    private void onlyCallService() {
        Context.require(!varXCallContract.getOrDefault(ZERO_ADDRESS).equals(ZERO_ADDRESS),
                "XCall contract is not configured.");
        Context.require(Context.getCaller().equals(varXCallContract.get()),
                "Caller is not the configured XCall contract  (" + varXCallContract.get().toString() + ")");
    }

    /**
     * Loops through the ArrayDB object to find a specific value
     * 
     * @param <T>     The class type of the array being checked
     * @param value   the value to check for
     * @param arraydb the array object to check
     * @return boolean indicating item is found (true), or not (false)
     */
    public static <T> Boolean containsInArrayDb(T value, ArrayDB<T> arraydb) {
        boolean found = false;
        if (arraydb == null || value == null) {
            return found;
        }

        for (int i = 0; i < arraydb.size(); i++) {
            if (arraydb.get(i) != null
                    && arraydb.get(i).equals(value)) {
                found = true;
                break;
            }
        }
        return found;
    }
}