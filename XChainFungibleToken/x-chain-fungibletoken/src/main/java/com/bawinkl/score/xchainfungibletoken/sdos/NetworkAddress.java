package com.bawinkl.score.xchainfungibletoken.sdos;

import java.util.Map;

import foundation.icon.score.data.ScoreDataObject;

import score.Context;
import score.Address;
import score.ObjectReader;
import score.ObjectWriter;

@ScoreDataObject
public class NetworkAddress {

    protected String networkID;
    protected String address;

    public NetworkAddress()
    {
        networkID = "";
        address = ""; 
    }

    /**
     * @param address The local network address by itself (ex: hxc5e0b88cb9092bbd8b004a517996139334752f62), a BTP address in the format of [btp://][networkID]/[address] or a network address without the leading btp component [networkID]/[address]
     * @param networkID
     */
    public NetworkAddress(String addr, String networkID)
    {
       parseNetworkAddress(addr, networkID);
    }

    /**
     * @param address The local network address by itself (ex: hxc5e0b88cb9092bbd8b004a517996139334752f62), a BTP address in the format of [btp://][networkID]/[address] or a network address without the leading btp component [networkID]/[address]
     * @param networkID
     */
    public NetworkAddress(Address addr, String networkID)
    {
       parseNetworkAddress(addr.toString(), networkID);
    }

    /**
     * Parses a network address from a BTP network address string [btp://][networkID]/[address] with the [btp://] componenet being optional.
     * will revert if address is null or blank
     * will revert if address does not meet one of the expectedc formats
     * @param addr Required; The local network address by itself, a BTP address in the format of [btp://][networkID]/[address] or a network address without the leading btp component [networkID]/[address]
     * @param networkID Optional; A network ID value, required only if the addr is in the local network format or a blank string
     */
    private void parseNetworkAddress(String addr, String netID)
    {
        //Force our address to a lower case variant, in case it wasn't
        addr = addr.toLowerCase();

        //Revert if the address is blank or null
        if(addr == null || addr.length() == 0)
            Context.revert("address cannot be null or blank when parsing a network address");

        //Remove the btp component if it exists
        if(addr.startsWith("btp://"))
        {
            addr = addr.substring(6);    
        }

        //Revert if the string does not contian the expected seperator ("/") or contains more than one
        if(addr.indexOf("/") < 0 && (netID == null || netID.length() == 0))
            Context.revert("address does not appear to be in the expected network address format ([btp://][networkID]/[address]) and a network ID was not provided.");
        else if (addr.indexOf("/") > -1 && addr.indexOf("/") != addr.lastIndexOf("/"))
             Context.revert("address does not appear to be in the expected btp or network address format ([btp://][networkID]/[address]).");

        //Parse the network and address components from the string or set them to the assigned values
        if(addr.indexOf("/") > -1)
        {
            networkID = addr.substring(0, addr.indexOf("/"));
            address = addr.substring(addr.indexOf("/") + 1);
        }
        else
        {
            networkID = netID;
            address = addr;
        }
    }

    public String getNetworkID()
    {
        return this.networkID;
    }

    public String setNetworkID(String value)
    {
        return this.networkID = value;
    }

    public String getAddress()
    {
        return this.address;
    }

    public String setAddress(String value)
    {
        return this.address = value;
    }

    /**
     * Writes the newtork address to an object using ObjectWriter
     * @param w the object writer
     * @param na the network address
     */
    public static void writeObject(ObjectWriter w, NetworkAddress na) {         
        
        w.beginList(1);
        w.write(na.networkID);
        w.write(na.address);
        w.end();
    }

    /**
     * Reads the network address object from an object reader
     * @param r the object reader
     */
    public static NetworkAddress readObject(ObjectReader r) {

        String networkID = "";
        String address = "";   

        r.beginList();
        if(r.hasNext())
        networkID = r.readString();
        if(r.hasNext())
        address = r.readString();
        r.end();

        return new NetworkAddress(networkID, address);
    }

    /**
     * Returns a string representation of a network address in [networkID]/[address] format
     */
    public String toString() {
        return networkID + "/" + address;
    }

    /**
     * Returns a map representation of the network address containing networkID and address keys
     */
    public Map<String, String> toMap() {
        Map<String, String> map = Map.of(   
                "networkID", this.networkID,
                "address", this.address);
            return map;
    }

     /**
      * Overriding equals() to compare two network address objects
      */
     @Override
     public boolean equals(Object o) {
  
         // If the object is compared with itself then return true 
         if (o == this) {
             return true;
         }
  
         /* Check if o is an instance of Complex or not
           "null instanceof [type]" also returns false */
         if (!(o instanceof NetworkAddress)) {
             return false;
         }
          
         // typecast o to Complex so that we can compare data members
         NetworkAddress c = (NetworkAddress)o;
          
         // Compare the data members and return accordingly
         return this.address.equals(c.address)
                 && this.networkID.equals(c.networkID);
     }
}
