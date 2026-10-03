package abyssredemption.performance.network;

import abyssredemption.AbsServerTool;
import abyssredemption.network.ProtocolConstants;
import abyssredemption.performance.PerformanceProfiler;
import abyssredemption.performance.ProfilerState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class PerformanceControlPayloads {
    private PerformanceControlPayloads() {}
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> packetType(String id) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(AbsServerTool.MOD_ID, id));
    }

    public record StartRequest(int protocolVersion, long requestId, int durationSeconds) implements CustomPacketPayload {
        public static final Type<StartRequest> TYPE = packetType("performance_start_request");
        public static final StreamCodec<RegistryFriendlyByteBuf, StartRequest> CODEC = StreamCodec.of(
                (b, p) -> { b.writeVarInt(p.protocolVersion()); b.writeVarLong(p.requestId()); b.writeVarInt(p.durationSeconds()); },
                b -> new StartRequest(b.readVarInt(), b.readVarLong(), b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record StartResponse(int protocolVersion, long requestId, long sessionId, int acceptedDurationSeconds,
                                long startedAtEpochMillis, long expectedEndEpochMillis) implements CustomPacketPayload {
        public static final Type<StartResponse> TYPE = packetType("performance_start_response");
        public static final StreamCodec<RegistryFriendlyByteBuf, StartResponse> CODEC = StreamCodec.of(
                (b, p) -> { b.writeVarInt(p.protocolVersion()); b.writeVarLong(p.requestId()); b.writeVarLong(p.sessionId());
                    b.writeVarInt(p.acceptedDurationSeconds()); b.writeLong(p.startedAtEpochMillis()); b.writeLong(p.expectedEndEpochMillis()); },
                b -> new StartResponse(b.readVarInt(), b.readVarLong(), b.readVarLong(), b.readVarInt(), b.readLong(), b.readLong()));
        public static StartResponse from(long requestId, PerformanceProfiler.StartResult result) {
            return new StartResponse(ProtocolConstants.PROTOCOL_VERSION, requestId, result.sessionId(), result.durationSeconds(),
                    result.startedAtMillis(), result.expectedEndMillis());
        }
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record StatusRequest(int protocolVersion, long requestId) implements CustomPacketPayload {
        public static final Type<StatusRequest> TYPE = packetType("performance_status_request");
        public static final StreamCodec<RegistryFriendlyByteBuf, StatusRequest> CODEC = StreamCodec.of(
                (b, p) -> { b.writeVarInt(p.protocolVersion()); b.writeVarLong(p.requestId()); },
                b -> new StatusRequest(b.readVarInt(), b.readVarLong()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record StatusResponse(int protocolVersion, long requestId, int state, long sessionId,
                                 int defaultDurationSeconds, int minimumDurationSeconds, int maximumDurationSeconds,
                                 int requestedDurationSeconds, long startedAtEpochMillis, long expectedEndEpochMillis,
                                 long elapsedMillis, long latestReportId) implements CustomPacketPayload {
        public static final Type<StatusResponse> TYPE = packetType("performance_status_response");
        public static final StreamCodec<RegistryFriendlyByteBuf, StatusResponse> CODEC = StreamCodec.of(
                (b, p) -> { b.writeVarInt(p.protocolVersion()); b.writeVarLong(p.requestId()); b.writeVarInt(p.state());
                    b.writeVarLong(p.sessionId()); b.writeVarInt(p.defaultDurationSeconds()); b.writeVarInt(p.minimumDurationSeconds());
                    b.writeVarInt(p.maximumDurationSeconds()); b.writeVarInt(p.requestedDurationSeconds());
                    b.writeLong(p.startedAtEpochMillis()); b.writeLong(p.expectedEndEpochMillis());
                    b.writeVarLong(p.elapsedMillis()); b.writeVarLong(p.latestReportId()); },
                b -> new StatusResponse(b.readVarInt(), b.readVarLong(), b.readVarInt(), b.readVarLong(), b.readVarInt(),
                        b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readLong(), b.readLong(), b.readVarLong(), b.readVarLong()));
        public static StatusResponse from(long requestId, PerformanceProfiler.Status status,
                                          abyssredemption.config.AbsServerConfig.PerformanceProfiler config) {
            return new StatusResponse(ProtocolConstants.PROTOCOL_VERSION, requestId, status.state().ordinal(),
                    status.sessionId(), config.defaultDurationSeconds(), config.minimumDurationSeconds(),
                    config.maximumDurationSeconds(), status.requestedSeconds(), status.startedAtMillis(),
                    status.expectedEndMillis(), status.elapsedMillis(), status.latestReportId());
        }
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record StopRequest(int protocolVersion, long requestId, long sessionId) implements CustomPacketPayload {
        public static final Type<StopRequest> TYPE = packetType("performance_stop_request");
        public static final StreamCodec<RegistryFriendlyByteBuf, StopRequest> CODEC = StreamCodec.of(
                (b, p) -> { b.writeVarInt(p.protocolVersion()); b.writeVarLong(p.requestId()); b.writeVarLong(p.sessionId()); },
                b -> new StopRequest(b.readVarInt(), b.readVarLong(), b.readVarLong()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record StopResponse(int protocolVersion, long requestId, long sessionId, long reportId,
                               long actualDurationMillis, boolean stoppedEarly) implements CustomPacketPayload {
        public static final Type<StopResponse> TYPE = packetType("performance_stop_response");
        public static final StreamCodec<RegistryFriendlyByteBuf, StopResponse> CODEC = StreamCodec.of(
                (b, p) -> { b.writeVarInt(p.protocolVersion()); b.writeVarLong(p.requestId()); b.writeVarLong(p.sessionId());
                    b.writeVarLong(p.reportId()); b.writeVarLong(p.actualDurationMillis()); b.writeBoolean(p.stoppedEarly()); },
                b -> new StopResponse(b.readVarInt(), b.readVarLong(), b.readVarLong(), b.readVarLong(), b.readVarLong(), b.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
