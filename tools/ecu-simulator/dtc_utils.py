# Legacy string maps preserved for backward compatibility
DTC_GROUP = {"P": "00", "C": "01", "B": "10", "U": "11"}
DTC_TYPE = {"0": "00", "1": "01", "2": "10", "3": "11"}

# Pre-computed bitwise lookup tables to avoid slow string parsing and try/except exceptions
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
    """Encodes a list of DTC strings into SAE J1979 Mode 03 OBD bytearray response."""
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if len(dtc) == DTC_LENGTH:
            g = DTC_GROUP_BITS.get(dtc[0])
            t = DTC_TYPE_BITS.get(dtc[1])
            h2 = HEX_VAL.get(dtc[2])
            h3 = HEX_VAL.get(dtc[3])
            h4 = HEX_VAL.get(dtc[4])
            if g is not None and t is not None and h2 is not None and h3 is not None and h4 is not None:
                dtcs_bytes.append(g | t | h2)
                dtcs_bytes.append((h3 << 4) | h4)
    return dtcs_bytes


def encode_uds_dtcs(dtcs):
    """Encodes a list of DTC strings into ISO 14229 UDS bytearray response."""
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if len(dtc) == DTC_LENGTH:
            g = DTC_GROUP_BITS.get(dtc[0])
            t = DTC_TYPE_BITS.get(dtc[1])
            h2 = HEX_VAL.get(dtc[2])
            h3 = HEX_VAL.get(dtc[3])
            h4 = HEX_VAL.get(dtc[4])
            if g is not None and t is not None and h2 is not None and h3 is not None and h4 is not None:
                dtcs_bytes.append(g | t | h2)
                dtcs_bytes.append((h3 << 4) | h4)
                dtcs_bytes.append(UDS_DTC_HIGH_BYTE)
                dtcs_bytes.append(UDS_DTC_DEFAULT_STATUS)
    return dtcs_bytes


def is_dtc_valid(dtc):
    """Fast validation of a DTC string without string parsing or exception throwing."""
    return (
        len(dtc) == DTC_LENGTH
        and dtc[0] in DTC_GROUP_BITS
        and dtc[1] in DTC_TYPE_BITS
        and dtc[2] in HEX_VAL
        and dtc[3] in HEX_VAL
        and dtc[4] in HEX_VAL
    )


def get_dtc_first_byte(dtc):
    """Encodes the first byte of a DTC using fast bitwise lookup."""
    g = DTC_GROUP_BITS.get(dtc[0], 0)
    t = DTC_TYPE_BITS.get(dtc[1], 0)
    h = HEX_VAL.get(dtc[2], 0)
    return bytes([g | t | h])


def get_dtc_second_byte(dtc):
    """Encodes the second byte of a DTC using fast bitwise lookup."""
    h3 = HEX_VAL.get(dtc[3], 0)
    h4 = HEX_VAL.get(dtc[4], 0)
    return bytes([(h3 << 4) | h4])


def is_hex_value(value):
    """Checks if a character is a valid hex digit using fast dict lookup."""
    return value in HEX_VAL
