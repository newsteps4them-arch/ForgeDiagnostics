DTC_GROUP = {"P": "00", "C": "01", "B": "10", "U": "11"}
DTC_TYPE = {"0": "00", "1": "01", "2": "10", "3": "11"}

# Pre-computed bitwise lookup tables to eliminate string formatting, parsing, and exception handling
DTC_GROUP_BITS = {"P": 0x00, "C": 0x40, "B": 0x80, "U": 0xC0}
DTC_TYPE_BITS = {"0": 0x00, "1": 0x10, "2": 0x20, "3": 0x30}
HEX_VAL = {c: int(c, 16) for c in "0123456789abcdefABCDEF"}

DTC_LENGTH = 5
BIG_ENDIAN = "big"
UDS_DTC_HIGH_BYTE = 0x01
UDS_DTC_DEFAULT_STATUS = 0x2F
UDS_DTC_SUFFIX = bytes([UDS_DTC_HIGH_BYTE, UDS_DTC_DEFAULT_STATUS])


def encode_obd_dtcs(dtcs):
    """Encodes a list of OBD-II DTC strings into raw binary bytearray representation."""
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            dtcs_bytes.append(DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]])
            dtcs_bytes.append((HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]])
    return dtcs_bytes


def encode_uds_dtcs(dtcs):
    """Encodes a list of UDS DTC strings into raw binary bytearray representation."""
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            dtcs_bytes.append(DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]])
            dtcs_bytes.append((HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]])
            dtcs_bytes.extend(UDS_DTC_SUFFIX)
    return dtcs_bytes


def is_dtc_valid(dtc):
    """Validates whether a DTC string follows the standard 5-character SAE J2012 format."""
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
    """Returns the first byte of an encoded DTC string as a bytes object."""
    group_val = DTC_GROUP_BITS.get(dtc[0], 0) if len(dtc) > 0 else 0
    type_val = DTC_TYPE_BITS.get(dtc[1], 0) if len(dtc) > 1 else 0
    hex_val = HEX_VAL.get(dtc[2], 0) if len(dtc) > 2 else 0
    return bytes([group_val | type_val | hex_val])


def get_dtc_second_byte(dtc):
    """Returns the second byte of an encoded DTC string as a bytes object."""
    val3 = HEX_VAL.get(dtc[3], 0) if len(dtc) > 3 else 0
    val4 = HEX_VAL.get(dtc[4], 0) if len(dtc) > 4 else 0
    return bytes([(val3 << 4) | val4])


def is_hex_value(value):
    """Fast lookup check if character is a valid hex digit (0-9, A-F, a-f)."""
    return value in HEX_VAL
