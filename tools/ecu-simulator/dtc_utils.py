# Pre-computed bitwise lookup dictionaries for high-frequency DTC encoding
# Bitwise layout for OBD DTC Byte 1:
# Bits 7-6: DTC Group (P=00, C=01, B=10, U=11) -> 0x00, 0x40, 0x80, 0xC0
# Bits 5-4: DTC Type (0=00, 1=01, 2=10, 3=11) -> 0x00, 0x10, 0x20, 0x30
# Bits 3-0: DTC Digit 2 (Hex 0-F)
DTC_GROUP = {"P": "00", "C": "01", "B": "10", "U": "11"}
DTC_TYPE = {"0": "00", "1": "01", "2": "10", "3": "11"}

DTC_GROUP_BITS = {"P": 0x00, "C": 0x40, "B": 0x80, "U": 0xC0}
DTC_TYPE_BITS = {"0": 0x00, "1": 0x10, "2": 0x20, "3": 0x30}

# Fast lookup dictionary for hex validation and nibble values (0-15)
HEX_VAL = {
    "0": 0, "1": 1, "2": 2, "3": 3, "4": 4,
    "5": 5, "6": 6, "7": 7, "8": 8, "9": 9,
    "a": 10, "b": 11, "c": 12, "d": 13, "e": 14, "f": 15,
    "A": 10, "B": 11, "C": 12, "D": 13, "E": 14, "F": 15,
}

DTC_LENGTH = 5

BIG_ENDIAN = "big"

UDS_DTC_HIGH_BYTE = 0x01

UDS_DTC_DEFAULT_STATUS = 0x2F

UDS_DTC_SUFFIX = bytes([UDS_DTC_HIGH_BYTE, UDS_DTC_DEFAULT_STATUS])


def encode_obd_dtcs(dtcs):
    """
    Encodes a list of OBD DTC strings into a raw bytearray.
    Optimized with pre-computed bitwise operations to bypass intermediate bytes allocations and string slicing.
    """
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            # Fast bitwise encoding: Byte 1 = Group | Type | Digit2, Byte 2 = (Digit3 << 4) | Digit4
            dtcs_bytes.append(DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]])
            dtcs_bytes.append((HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]])
    return dtcs_bytes


def encode_uds_dtcs(dtcs):
    """
    Encodes a list of UDS DTC strings into a raw bytearray with status bytes.
    Optimized with pre-computed bitwise operations to bypass intermediate bytes allocations and string slicing.
    """
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            dtcs_bytes.append(DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]])
            dtcs_bytes.append((HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]])
            dtcs_bytes.extend(UDS_DTC_SUFFIX)
    return dtcs_bytes


def is_dtc_valid(dtc):
    """
    Validates DTC string format using O(1) dictionary lookups instead of try/except int(..., 16).
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
    Extracts the first encoded byte of a DTC using bitwise OR operations.
    """
    return bytes([DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]]])


def get_dtc_second_byte(dtc):
    """
    Extracts the second encoded byte of a DTC using bit shifting.
    """
    return bytes([(HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]]])


def is_hex_value(value):
    """
    Validates if a single character is a valid hexadecimal character via O(1) table lookup.
    """
    return len(value) == 1 and value in HEX_VAL
