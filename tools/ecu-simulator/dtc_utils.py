DTC_GROUP = {"P": "00", "C": "01", "B": "10", "U": "11"}

DTC_TYPE = {"0": "00", "1": "01", "2": "10", "3": "11"}

DTC_LENGTH = 5

BIG_ENDIAN = "big"

UDS_DTC_HIGH_BYTE = 0x01

UDS_DTC_DEFAULT_STATUS = 0x2F

# Fast O(1) bitwise lookup tables bypassing string parsing & try/except overhead
HEX_VAL = {c: int(c, 16) for c in "0123456789ABCDEFabcdef"}
DTC_GROUP_BITS = {"P": 0x00, "C": 0x40, "B": 0x80, "U": 0xC0}
DTC_TYPE_BITS = {"0": 0x00, "1": 0x01 << 4, "2": 0x02 << 4, "3": 0x03 << 4}


def encode_obd_dtcs(dtcs):
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            dtcs_bytes.append(DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]])
            dtcs_bytes.append((HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]])
    return dtcs_bytes


def encode_uds_dtcs(dtcs):
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            dtcs_bytes.append(DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]])
            dtcs_bytes.append((HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]])
            dtcs_bytes.append(UDS_DTC_HIGH_BYTE)
            dtcs_bytes.append(UDS_DTC_DEFAULT_STATUS)
    return dtcs_bytes


def is_dtc_valid(dtc):
    return (len(dtc) == DTC_LENGTH and
            dtc[0] in DTC_GROUP_BITS and
            dtc[1] in DTC_TYPE_BITS and
            dtc[2] in HEX_VAL and
            dtc[3] in HEX_VAL and
            dtc[4] in HEX_VAL)


def get_dtc_first_byte(dtc):
    return bytes([DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]]])


def get_dtc_second_byte(dtc):
    return bytes([(HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]]])


def is_hex_value(value):
    return isinstance(value, str) and len(value) > 0 and all(c in HEX_VAL for c in value)
