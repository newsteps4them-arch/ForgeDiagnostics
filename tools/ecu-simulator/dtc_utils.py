# Pre-computed bitwise lookup tables to bypass string parsing and exception handling overhead
DTC_GROUP = {"P": "00", "C": "01", "B": "10", "U": "11"}
DTC_TYPE = {"0": "00", "1": "01", "2": "10", "3": "11"}

# Pre-computed bit-shifted lookup tables for ultra-fast DTC byte encoding
DTC_GROUP_BITS = {"P": 0x00, "C": 0x40, "B": 0x80, "U": 0xC0}
DTC_TYPE_BITS = {"0": 0x00, "1": 0x10, "2": 0x20, "3": 0x30}
HEX_VAL = {c: int(c, 16) for c in "0123456789abcdefABCDEF"}

DTC_LENGTH = 5
BIG_ENDIAN = "big"
UDS_DTC_HIGH_BYTE = 0x01
UDS_DTC_DEFAULT_STATUS = 0x2F


def encode_obd_dtcs(dtcs):
    """
    Fast SAE J1979 Mode 03 DTC encoder.
    Uses pre-computed bitwise lookups to avoid string concatenation and exception handling.
    """
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            b1 = DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]]
            b2 = (HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]]
            dtcs_bytes.append(b1)
            dtcs_bytes.append(b2)
    return dtcs_bytes


def encode_uds_dtcs(dtcs):
    """
    Fast UDS Service 0x19 DTC encoder.
    Uses pre-computed bitwise lookups to avoid string concatenation and exception handling.
    """
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            b1 = DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]]
            b2 = (HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]]
            dtcs_bytes.append(b1)
            dtcs_bytes.append(b2)
            dtcs_bytes.append(UDS_DTC_HIGH_BYTE)
            dtcs_bytes.append(UDS_DTC_DEFAULT_STATUS)
    return dtcs_bytes


def is_dtc_valid(dtc):
    """
    Fast DTC validator using direct dictionary key lookups instead of try/except int conversion.
    """
    return (len(dtc) == DTC_LENGTH and
            dtc[0] in DTC_GROUP_BITS and
            dtc[1] in DTC_TYPE_BITS and
            dtc[2] in HEX_VAL and
            dtc[3] in HEX_VAL and
            dtc[4] in HEX_VAL)


def get_dtc_first_byte(dtc):
    """
    Fast calculation of the first byte of an encoded DTC.
    """
    return bytes([DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]]])


def get_dtc_second_byte(dtc):
    """
    Fast calculation of the second byte of an encoded DTC.
    """
    return bytes([(HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]]])


def is_hex_value(value):
    """
    Fast hex character validator using lookup set/dict.
    """
    return value in HEX_VAL
