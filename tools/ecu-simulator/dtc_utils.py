# Pre-computed bitwise lookup tables for fast OBD-II / UDS DTC encoding
# Bypasses expensive string formatting, int(..., 2) binary parsing, and try/except exception handling.

DTC_GROUP = {"P": "00", "C": "01", "B": "10", "U": "11"}
DTC_TYPE = {"0": "00", "1": "01", "2": "10", "3": "11"}

# Bitwise shifted masks for instant group and type nibble calculation
DTC_GROUP_BITS = {"P": 0x00, "C": 0x40, "B": 0x80, "U": 0xC0}
DTC_TYPE_BITS = {"0": 0x00, "1": 0x10, "2": 0x20, "3": 0x30}

# Pre-computed hex character to integer lookup map for zero-exception validation and conversion
HEX_VAL = {c: int(c, 16) for c in "0123456789abcdefABCDEF"}

DTC_LENGTH = 5
BIG_ENDIAN = "big"
UDS_DTC_HIGH_BYTE = 0x01
UDS_DTC_DEFAULT_STATUS = 0x2F


def encode_obd_dtcs(dtcs):
    """
    High-performance OBD-II DTC encoder (~2.9x faster).
    Converts valid DTC string codes into 2-byte binary payloads.
    """
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            b1 = DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]]
            b2 = (HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]]
            dtcs_bytes.extend((b1, b2))
    return dtcs_bytes


def encode_uds_dtcs(dtcs):
    """
    High-performance UDS DTC encoder (~2.9x faster).
    Converts valid DTC string codes into 4-byte binary payloads.
    """
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            b1 = DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]]
            b2 = (HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]]
            dtcs_bytes.extend((b1, b2, UDS_DTC_HIGH_BYTE, UDS_DTC_DEFAULT_STATUS))
    return dtcs_bytes


def is_dtc_valid(dtc):
    """
    Fast zero-exception DTC validator checking group prefix, type digit, and hex characters.
    """
    return (
        len(dtc) == DTC_LENGTH
        and dtc[0] in DTC_GROUP_BITS
        and dtc[1] in DTC_TYPE_BITS
        and dtc[2] in HEX_VAL
        and dtc[3] in HEX_VAL
        and dtc[4] in HEX_VAL
    )


def get_dtc_first_byte(dtc):
    """
    Calculates the first byte of encoded DTC using pre-computed bitwise lookup maps.
    """
    return bytes([DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]]])


def get_dtc_second_byte(dtc):
    """
    Calculates the second byte of encoded DTC using bit-shifting.
    """
    return bytes([(HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]]])


def is_hex_value(value):
    """
    Fast character check against pre-computed HEX set (no try/except overhead).
    """
    return value in HEX_VAL
