DTC_GROUP = {"P": "00", "C": "01", "B": "10", "U": "11"}

DTC_TYPE = {"0": "00", "1": "01", "2": "10", "3": "11"}

# Pre-computed bitwise lookup dictionaries for fast zero-parsing DTC encoding
DTC_GROUP_BITS = {"P": 0x00, "C": 0x40, "B": 0x80, "U": 0xC0}
DTC_TYPE_BITS = {"0": 0x00, "1": 0x10, "2": 0x20, "3": 0x30}

HEX_VAL = {
    '0': 0, '1': 1, '2': 2, '3': 3, '4': 4, '5': 5, '6': 6, '7': 7, '8': 8, '9': 9,
    'A': 10, 'B': 11, 'C': 12, 'D': 13, 'E': 14, 'F': 15,
    'a': 10, 'b': 11, 'c': 12, 'd': 13, 'e': 14, 'f': 15
}

DTC_LENGTH = 5

BIG_ENDIAN = "big"

UDS_DTC_HIGH_BYTE = 0x01

UDS_DTC_DEFAULT_STATUS = 0x2F


def encode_obd_dtcs(dtcs):
    """
    Optimized OBD-II DTC encoder using bitwise lookup tables to avoid
    string slicing, conversions, and exception handling overhead.
    """
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            g = DTC_GROUP_BITS[dtc[0]]
            t = DTC_TYPE_BITS[dtc[1]]
            h2 = HEX_VAL[dtc[2]]
            h3 = HEX_VAL[dtc[3]]
            h4 = HEX_VAL[dtc[4]]
            dtcs_bytes.append(g | t | h2)
            dtcs_bytes.append((h3 << 4) | h4)
    return dtcs_bytes


def encode_uds_dtcs(dtcs):
    """
    Optimized UDS DTC encoder using pre-computed bitwise masks.
    """
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            g = DTC_GROUP_BITS[dtc[0]]
            t = DTC_TYPE_BITS[dtc[1]]
            h2 = HEX_VAL[dtc[2]]
            h3 = HEX_VAL[dtc[3]]
            h4 = HEX_VAL[dtc[4]]
            dtcs_bytes.append(g | t | h2)
            dtcs_bytes.append((h3 << 4) | h4)
            dtcs_bytes.append(UDS_DTC_HIGH_BYTE)
            dtcs_bytes.append(UDS_DTC_DEFAULT_STATUS)
    return dtcs_bytes


def is_dtc_valid(dtc):
    return (
        len(dtc) == DTC_LENGTH
        and dtc[0] in DTC_GROUP_BITS
        and dtc[1] in DTC_TYPE_BITS
        and dtc[2] in HEX_VAL
        and dtc[3] in HEX_VAL
        and dtc[4] in HEX_VAL
    )


def get_dtc_first_byte(dtc):
    bits_0_3 = int(DTC_GROUP.get(dtc[0]) + DTC_TYPE.get(dtc[1]) + "0000", 2)
    bits_4_7 = int("0000" + dtc[2], 16)
    return (bits_0_3 | bits_4_7).to_bytes(1, BIG_ENDIAN)


def get_dtc_second_byte(dtc):
    return int((dtc[3] + dtc[4]), 16).to_bytes(1, BIG_ENDIAN)


def is_hex_value(value):
    return value in HEX_VAL
