DTC_GROUP = {"P": "00", "C": "01", "B": "10", "U": "11"}

DTC_TYPE = {"0": "00", "1": "01", "2": "10", "3": "11"}

# Pre-computed bitwise lookup tables to eliminate string parsing, base conversion, and try/except overhead
DTC_GROUP_BITS = {"P": 0x00, "C": 0x40, "B": 0x80, "U": 0xC0}
DTC_TYPE_BITS = {"0": 0x00, "1": 0x10, "2": 0x20, "3": 0x30}

HEX_VAL = {
    "0": 0, "1": 1, "2": 2, "3": 3, "4": 4, "5": 5, "6": 6, "7": 7, "8": 8, "9": 9,
    "A": 10, "B": 11, "C": 12, "D": 13, "E": 14, "F": 15,
    "a": 10, "b": 11, "c": 12, "d": 13, "e": 14, "f": 15
}

DTC_LENGTH = 5

BIG_ENDIAN = "big"

UDS_DTC_HIGH_BYTE = 0x01

UDS_DTC_DEFAULT_STATUS = 0x2F


def encode_obd_dtcs(dtcs):
    """Encodes OBD Diagnostic Trouble Codes into raw byte sequences using bitwise lookup tables."""
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            b1 = DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]]
            b2 = (HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]]
            dtcs_bytes.extend((b1, b2))
    return dtcs_bytes


def encode_uds_dtcs(dtcs):
    """Encodes UDS Diagnostic Trouble Codes into raw byte sequences using bitwise lookup tables."""
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            b1 = DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]]
            b2 = (HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]]
            dtcs_bytes.extend((b1, b2, UDS_DTC_HIGH_BYTE, UDS_DTC_DEFAULT_STATUS))
    return dtcs_bytes


def is_dtc_valid(dtc):
    """Fast validation of DTC format using direct hash lookup instead of try/except parsing."""
    return (
        isinstance(dtc, str)
        and len(dtc) == DTC_LENGTH
        and dtc[0] in DTC_GROUP_BITS
        and dtc[1] in DTC_TYPE_BITS
        and dtc[2] in HEX_VAL
        and dtc[3] in HEX_VAL
        and dtc[4] in HEX_VAL
    )


def get_dtc_first_byte(dtc):
    """Computes the first byte of an encoded DTC."""
    b1 = DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]]
    return bytes([b1])


def get_dtc_second_byte(dtc):
    """Computes the second byte of an encoded DTC."""
    b2 = (HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]]
    return bytes([b2])


def is_hex_value(value):
    """Checks if a string consists entirely of valid hex digits."""
    return isinstance(value, str) and len(value) > 0 and all(c in HEX_VAL for c in value)
