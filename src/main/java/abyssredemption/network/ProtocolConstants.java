package abyssredemption.network;

public final class ProtocolConstants {
    public static final int PROTOCOL_VERSION = 1;
    public static final int CAP_PLAYTIME = 1;
    public static final int CAP_DEATHS = 1 << 1;
    public static final int CAP_BLOCKS_TOTAL = 1 << 2;
    public static final int CAP_BLOCKS_WEEKLY = 1 << 3;
    public static final int ALL_CAPABILITIES = CAP_PLAYTIME | CAP_DEATHS | CAP_BLOCKS_TOTAL | CAP_BLOCKS_WEEKLY;
    private ProtocolConstants() {}
}
