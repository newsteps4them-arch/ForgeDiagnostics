# Pre-computed bitwise lookup dictionaries for high-performance DTC encoding
DTC_GROUP_BITS = {"P": 0x00, "C": 0x40, "B": 0x80, "U": 0xC0}
DTC_TYPE_BITS = {"0": 0x00, "1": 0x01, "2": 0x02, "3": 0x03}
HEX_VAL = {
    "0": 0, "1": 1, "2": 2, "3": 3, "4": 4, "5": 5, "6": 6, "7": 7,
    "8": 8, "9": 9, "a": 10, "b": 11, "c": 12, "d": 13, "e": 14, "f": 15,
    "A": 10, "B": 11, "C": 12, "D": 13, "E": 14, "F": 15
}

# Legacy string mapping maintained for backward compatibility if imported elsewhere
DTC_GROUP = {"P": "00", "C": "01", "B": "10", "U": "11"}
DTC_TYPE = {"0": "00", "1": "01", "2": "10", "3": "11"}

DTC_LENGTH = 5
BIG_ENDIAN = "big"
UDS_DTC_HIGH_BYTE = 0x01
UDS_DTC_DEFAULT_STATUS = 0x2F


def encode_obd_dtcs(dtcs):
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            # Fast bitwise encoding without string parsing or int(..., 2)
            b1 = DTC_GROUP_BITS[dtc[0]] | (DTC_TYPE_BITS[dtc[1]] << 4) | HEX_VAL[dtc[2]]
            b2 = (HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]]
            dtcs_bytes.extend((b1, b2))
    return bytes(dtcs_bytes)


def encode_uds_dtcs(dtcs):
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            # Fast bitwise encoding without string parsing or int(..., 2)
            b1 = DTC_GROUP_BITS[dtc[0]] | (DTC_TYPE_BITS[dtc[1]] << 4) | HEX_VAL[dtc[2]]
            b2 = (HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]]
            dtcs_bytes.extend((b1, b2, UDS_DTC_HIGH_BYTE, UDS_DTC_DEFAULT_STATUS))
    return bytes(dtcs_bytes)


def is_dtc_valid(dtc):
    if len(dtc) != DTC_LENGTH:
        return False
    return (
        dtc[0] in DTC_GROUP_BITS
        and dtc[1] in DTC_TYPE_BITS
        and dtc[2] in HEX_VAL
        and dtc[3] in HEX_VAL
        and dtc[4] in HEX_VAL
    )


def get_dtc_first_byte(dtc):
    b1 = DTC_GROUP_BITS[dtc[0]] | (DTC_TYPE_BITS[dtc[1]] << 4) | HEX_VAL[dtc[2]]
    return bytes([b1])


def get_dtc_second_byte(dtc):
    b2 = (HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]]
    return bytes([b2])


def is_hex_value(value):
    if not value or not isinstance(value, str):
        return False
    return all(c in HEX_VAL for c in value)
