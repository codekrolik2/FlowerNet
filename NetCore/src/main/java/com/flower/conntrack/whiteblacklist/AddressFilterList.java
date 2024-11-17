package com.flower.conntrack.whiteblacklist;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableAddressFilterList.class)
@JsonDeserialize(as = ImmutableAddressFilterList.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface AddressFilterList {
    @Value.Immutable
    @JsonSerialize(as = ImmutableAddressRecord.class)
    @JsonDeserialize(as = ImmutableAddressRecord.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    interface AddressRecord {
        FilterType filterType();
        String dstHost();
        Integer dstPort();
        Long creationTimestamp();
        Boolean isWildcard();

        static boolean recordsEqual(@Nullable AddressRecord record1, @Nullable AddressRecord record2) {
            if (record1 == record2) { return true; }
            if (record1 == null || record2 == null) { return false; }

            return record1.filterType().equals(record2.filterType())
                    && record1.dstHost().equals(record2.dstHost())
                    && record1.dstPort().equals(record2.dstPort());
        }
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableHostRecord.class)
    @JsonDeserialize(as = ImmutableHostRecord.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    interface HostRecord {
        FilterType filterType();
        String dstHost();
        Long creationTimestamp();
        Boolean isWildcard();

        static boolean recordsEqual(HostRecord record1, HostRecord record2) {
            if (record1 == record2) { return true; }
            if (record1 == null || record2 == null) { return false; }

            return record1.filterType().equals(record2.filterType())
                    && record1.dstHost().equals(record2.dstHost());
        }
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutablePortRecord.class)
    @JsonDeserialize(as = ImmutablePortRecord.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    interface PortRecord {
        FilterType filterType();
        Integer dstPort();
        Long creationTimestamp();

        static boolean recordsEqual(PortRecord record1, PortRecord record2) {
            if (record1 == record2) { return true; }
            if (record1 == null || record2 == null) { return false; }

            return record1.filterType().equals(record2.filterType())
                    && record1.dstPort().equals(record2.dstPort());
        }
    }

    List<AddressRecord> addressRecords();
    List<HostRecord> hostRecords();
    List<PortRecord> portRecords();

    List<AddressRecord> wildcardAddressRecords();
    List<HostRecord> wildcardHostRecords();
}
