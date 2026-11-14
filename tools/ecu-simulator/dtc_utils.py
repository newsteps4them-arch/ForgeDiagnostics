DTC_GROUP = {"P": "00", "C": "01", "B": "10", "U": "11"}

DTC_TYPE = {"0": "00", "1": "01", "2": "10", "3": "11"}

DTC_LENGTH = 5

BIG_ENDIAN = "big"

UDS_DTC_HIGH_BYTE = 0x01

UDS_DTC_DEFAULT_STATUS = 0x2F

# Pre-computed bitwise lookup tables for fast DTC encoding and validation without string parsing/exception overhead:
DTC_GROUP_BITS = {"P": 0x00, "C": 0x40, "B": 0x80, "U": 0xC0}

DTC_TYPE_BITS = {"0": 0x00, "1": 0x10, "2": 0x20, "3": 0x30}

HEX_VAL = {
    "0": 0, "1": 1, "2": 2, "3": 3, "4": 4, "5": 5, "6": 6, "7": 7, "8": 8, "9": 9,
    "a": 10, "b": 11, "c": 12, "d": 13, "e": 14, "f": 15,
    "A": 10, "B": 11, "C": 12, "D": 13, "E": 14, "F": 15
}

UDS_SUFFIX = bytes([UDS_DTC_HIGH_BYTE, UDS_DTC_DEFAULT_STATUS])


def encode_obd_dtcs(dtcs):
    """
    Fast OBD-II DTC encoder using pre-computed bitwise lookups.
    """
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            dtcs_bytes += get_dtc_first_byte(dtc) + get_dtc_second_byte(dtc)
    return dtcs_bytes


def encode_uds_dtcs(dtcs):
    """
    Fast UDS DTC encoder using pre-computed bitwise lookups.
    """
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            dtcs_bytes += get_dtc_first_byte(dtc) + get_dtc_second_byte(dtc) + UDS_SUFFIX
    return dtcs_bytes


def is_dtc_valid(dtc):
    """
    Optimized DTC validation bypassing string parsing and try/except handling.
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
    Bitwise calculation of the first DTC byte from lookup tables.
    """
    return bytes([DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]]])


def get_dtc_second_byte(dtc):
    """
    Bitwise calculation of the second DTC byte from lookup tables.
    """
    return bytes([(HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]]])


def is_hex_value(value):
    """
    Checks if character(s) are valid hexadecimal digits.
    """
    if len(value) == 1:
        return value in HEX_VAL
    if not value:
        return False
    return all(c in HEX_VAL for c in value)
