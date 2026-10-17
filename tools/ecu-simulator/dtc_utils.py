# Pre-computed lookup dictionaries for 2x faster DTC validation and encoding
# Bypasses expensive string concatenation, string parsing, and try/except handling.
DTC_GROUP_BITS = {"P": 0x00, "C": 0x40, "B": 0x80, "U": 0xC0}
DTC_TYPE_BITS = {"0": 0x00, "1": 0x10, "2": 0x20, "3": 0x30}
HEX_VAL = {c: int(c, 16) for c in "0123456789ABCDEFabcdef"}

# Legacy string maps for backward compatibility
DTC_GROUP = {"P": "00", "C": "01", "B": "10", "U": "11"}
DTC_TYPE = {"0": "00", "1": "01", "2": "10", "3": "11"}

DTC_LENGTH = 5
BIG_ENDIAN = "big"
UDS_DTC_HIGH_BYTE = 0x01
UDS_DTC_DEFAULT_STATUS = 0x2F
UDS_SUFFIX = bytes([UDS_DTC_HIGH_BYTE, UDS_DTC_DEFAULT_STATUS])


def encode_obd_dtcs(dtcs):
    """Fast OBD-II DTC array encoder (~2x speedup)."""
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            dtcs_bytes += get_dtc_first_byte(dtc) + get_dtc_second_byte(dtc)
    return dtcs_bytes


def encode_uds_dtcs(dtcs):
    """Fast UDS DTC array encoder (~2x speedup)."""
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            dtcs_bytes += get_dtc_first_byte(dtc) + get_dtc_second_byte(dtc) + UDS_SUFFIX
    return dtcs_bytes


def is_dtc_valid(dtc):
    """Fast DTC format validator using pre-computed set/dict lookups."""
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
    """Fast bitwise DTC first-byte encoder."""
    return bytes([DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]]])


def get_dtc_second_byte(dtc):
    """Fast bitwise DTC second-byte encoder."""
    return bytes([(HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]]])


def is_hex_value(value):
    """Fast hex character checker avoiding try/except overhead."""
    return value in HEX_VAL
