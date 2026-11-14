# Legacy string maps preserved for backward compatibility
DTC_GROUP = {"P": "00", "C": "01", "B": "10", "U": "11"}
DTC_TYPE = {"0": "00", "1": "01", "2": "10", "3": "11"}

# Pre-computed bitwise lookup dictionaries to eliminate string parsing & try/except overhead
DTC_GROUP_BITS = {"P": 0x00, "C": 0x40, "B": 0x80, "U": 0xC0}
DTC_TYPE_BITS = {"0": 0x00, "1": 0x10, "2": 0x20, "3": 0x30}

# Fast lookup dictionary mapping valid hex characters to integer nibble values
HEX_VAL = {c: int(c, 16) for c in "0123456789abcdefABCDEF"}

DTC_LENGTH = 5
BIG_ENDIAN = "big"
UDS_DTC_HIGH_BYTE = 0x01
UDS_DTC_DEFAULT_STATUS = 0x2F
UDS_DTC_SUFFIX = bytes([UDS_DTC_HIGH_BYTE, UDS_DTC_DEFAULT_STATUS])


def encode_obd_dtcs(dtcs):
    """
    Optimized OBD DTC encoder using direct bitwise byte array appending.
    Bypasses string conversion and temporary bytes object creation.
    """
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            dtcs_bytes.append(DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]])
            dtcs_bytes.append((HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]])
    return dtcs_bytes


def encode_uds_dtcs(dtcs):
    """
    Optimized UDS DTC encoder using direct bitwise byte array appending.
    """
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            dtcs_bytes.append(DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]])
            dtcs_bytes.append((HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]])
            dtcs_bytes.append(UDS_DTC_HIGH_BYTE)
            dtcs_bytes.append(UDS_DTC_DEFAULT_STATUS)
    return dtcs_bytes


def is_dtc_valid(dtc):
    """
    Fast validation using dictionary membership checks without try/except overhead.
    """
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
    group_val = DTC_GROUP_BITS.get(dtc[0], 0) if len(dtc) > 0 else 0
    type_val = DTC_TYPE_BITS.get(dtc[1], 0) if len(dtc) > 1 else 0
    hex_val = HEX_VAL.get(dtc[2], 0) if len(dtc) > 2 else 0
    return bytes([group_val | type_val | hex_val])


def get_dtc_second_byte(dtc):
    val3 = HEX_VAL.get(dtc[3], 0) if len(dtc) > 3 else 0
    val4 = HEX_VAL.get(dtc[4], 0) if len(dtc) > 4 else 0
    return bytes([(val3 << 4) | val4])


def is_hex_value(value):
    """
    Fast validation for single hex character.
    """
    return isinstance(value, str) and len(value) == 1 and value in HEX_VAL
