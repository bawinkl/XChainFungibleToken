package com.bawinkl.score.xchainfungibletoken.sdos;

import java.lang.String;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import score.annotation.Keep;

public final class NetworkAddressSdo extends NetworkAddress {
  public NetworkAddressSdo() {
    super();
  }

  public NetworkAddressSdo(NetworkAddress obj) {
    super();
    this.setNetworkID(obj.getNetworkID());
    this.setAddress(obj.getAddress());
  }

  @Keep
  public static void writeObject(ObjectWriter writer, NetworkAddress obj) {
    NetworkAddressSdo.writeObject(writer, obj instanceof NetworkAddressSdo ? (NetworkAddressSdo)obj : new NetworkAddressSdo(obj));
  }

  public static void writeObject(ObjectWriter writer, NetworkAddressSdo obj) {
    obj.writeObject(writer);
  }

  public static NetworkAddressSdo readObject(ObjectReader reader) {
    NetworkAddressSdo obj = new NetworkAddressSdo();
    reader.beginList();
    obj.setNetworkID(reader.readNullable(String.class));
    obj.setAddress(reader.readNullable(String.class));
    reader.end();
    return obj;
  }

  public void writeObject(ObjectWriter writer) {
    writer.beginList(2);
    writer.writeNullable(this.getNetworkID());
    writer.writeNullable(this.getAddress());
    writer.end();
  }

  public static NetworkAddressSdo fromBytes(byte[] bytes) {
    ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
    return NetworkAddressSdo.readObject(reader);
  }

  public byte[] toBytes() {
    ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
    NetworkAddressSdo.writeObject(writer, this);
    return writer.toByteArray();
  }

  public static byte[] toBytes(NetworkAddress obj) {
    return obj instanceof NetworkAddressSdo ? ((NetworkAddressSdo)obj).toBytes() : new NetworkAddressSdo(obj).toBytes();
  }

  public String toString() {
    return super.toString();
  }
}
