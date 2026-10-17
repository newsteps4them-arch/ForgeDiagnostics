"""
Optimized DTC Utility module for SAE J1979 OBD-II and ISO 14229 UDS Diagnostic Trouble Codes.
Uses precomputed bitwise lookup maps to avoid string parsing and exception handling overhead.
"""

# Pre-calculated bitwise values for DTC prefixes (P=0x00, C=0x40, B=0x80, U=0xC0)
DTC_GROUP_BITS = {
    "P": 0x00, "p": 0x00,
    "C": 0x40, "c": 0x40,
    "B": 0x80, "b": 0x80,
    "U": 0xC0, "u": 0xC0,
}

# Pre-calculated bitwise values for DTC type digits (0=0x00, 1=0x10, 2=0x20, 3=0x30)
DTC_TYPE_BITS = {
    "0": 0x00,
    "1": 0x10,
    "2": 0x20,
    "3": 0x30
}

# Fast lookup for hex characters avoiding string allocations and try/except parsing overhead
HEX_VAL = {
    '0': 0, '1': 1, '2': 2, '3': 3, '4': 4, '5': 5, '6': 6, '7': 7, '8': 8, '9': 9,
    'A': 10, 'B': 11, 'C': 12, 'D': 13, 'E': 14, 'F': 15,
    'a': 10, 'b': 11, 'c': 12, 'd': 13, 'e': 14, 'f': 15
}

# Backwards compatibility dictionaries
DTC_GROUP = {"P": "00", "C": "01", "B": "10", "U": "11"}
DTC_TYPE = {"0": "00", "1": "01", "2": "10", "3": "11"}

DTC_LENGTH = 5
BIG_ENDIAN = "big"
UDS_DTC_HIGH_BYTE = 0x01
UDS_DTC_DEFAULT_STATUS = 0x2F


def encode_obd_dtcs(dtcs):
    """
    Encodes list of DTC strings into SAE J1979 OBD-II 2-byte response format.
    Fast bytearray construction avoids intermediate string allocations.
    """
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            dtcs_bytes += get_dtc_first_byte(dtc) + get_dtc_second_byte(dtc)
    return dtcs_bytes


def encode_uds_dtcs(dtcs):
    """
    Encodes list of DTC strings into ISO 14229 UDS 4-byte response format.
    """
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            dtcs_bytes += get_dtc_first_byte(dtc) + get_dtc_second_byte(dtc) + bytes([UDS_DTC_HIGH_BYTE, UDS_DTC_DEFAULT_STATUS])
    return dtcs_bytes


def is_dtc_valid(dtc):
    """
    Validates DTC string structure (e.g. 'P0300').
    Direct lookup set membership check bypasses try/except exception overhead.
    """
    return (
        isinstance(dtc, str) and
        len(dtc) == DTC_LENGTH and
        dtc[0] in DTC_GROUP_BITS and
        dtc[1] in DTC_TYPE_BITS and
        dtc[2] in HEX_VAL and
        dtc[3] in HEX_VAL and
        dtc[4] in HEX_VAL
    )


def get_dtc_first_byte(dtc):
    """
    Calculates the first byte of encoded DTC using precomputed bitwise OR operations.
    """
    val = DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]]
    return bytes([val])


def get_dtc_second_byte(dtc):
    """
    Calculates the second byte of encoded DTC using nibble bit-shifts.
    """
    val = (HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]]
    return bytes([val])


def is_hex_value(value):
    """
    Fast O(1) hex character check without exception handling.
    """
    return value in HEX_VAL
