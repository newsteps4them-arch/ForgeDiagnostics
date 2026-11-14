# Optimized bitwise lookup tables and constants for DTC encoding in ECU simulator
DTC_GROUP = {"P": "00", "C": "01", "B": "10", "U": "11"}

DTC_TYPE = {"0": "00", "1": "01", "2": "10", "3": "11"}

DTC_LENGTH = 5

BIG_ENDIAN = "big"

UDS_DTC_HIGH_BYTE = 0x01

UDS_DTC_DEFAULT_STATUS = 0x2F

# Pre-computed bitwise values and hex character map for high-frequency DTC encoding:
# Eliminates int(s, 2), int(s, 16), string slicing, and ValueError try/except overhead.
DTC_GROUP_BITS = {"P": 0x00, "C": 0x40, "B": 0x80, "U": 0xC0}

DTC_TYPE_BITS = {"0": 0x00, "1": 0x10, "2": 0x20, "3": 0x30}

HEX_VAL = {
    "0": 0, "1": 1, "2": 2, "3": 3, "4": 4, "5": 5, "6": 6, "7": 7, "8": 8, "9": 9,
    "a": 10, "b": 11, "c": 12, "d": 13, "e": 14, "f": 15,
    "A": 10, "B": 11, "C": 12, "D": 13, "E": 14, "F": 15,
}


def encode_obd_dtcs(dtcs):
    # Pre-allocates or extends single bytearray buffer directly with bitwise values
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            b1 = DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]]
            b2 = (HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]]
            dtcs_bytes.extend((b1, b2))
    return dtcs_bytes


def encode_uds_dtcs(dtcs):
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            b1 = DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]]
            b2 = (HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]]
            dtcs_bytes.extend((b1, b2, UDS_DTC_HIGH_BYTE, UDS_DTC_DEFAULT_STATUS))
    return dtcs_bytes


def is_dtc_valid(dtc):
    # Fast O(1) hash table lookups without string conversions or exception handling
    return len(dtc) == DTC_LENGTH and dtc[0] in DTC_GROUP_BITS and dtc[1] in DTC_TYPE_BITS \
           and dtc[2] in HEX_VAL and dtc[3] in HEX_VAL and dtc[4] in HEX_VAL


def get_dtc_first_byte(dtc):
    # Direct bitwise combination: group (bits 7-6), type (bits 5-4), hex digit 1 (bits 3-0)
    return bytes([(DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]])])


def get_dtc_second_byte(dtc):
    # Direct bitwise combination: hex digit 2 (bits 7-4), hex digit 3 (bits 3-0)
    return bytes([((HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]])])


def is_hex_value(value):
    # Fast dictionary lookup bypassing ValueError exception generation
    return value in HEX_VAL
