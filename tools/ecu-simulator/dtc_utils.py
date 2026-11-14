DTC_GROUP_BITS = {"P": 0x00, "C": 0x40, "B": 0x80, "U": 0xC0}
DTC_TYPE_BITS = {"0": 0x00, "1": 0x10, "2": 0x20, "3": 0x30}
HEX_VAL = {c: int(c, 16) for c in "0123456789abcdefABCDEF"}

DTC_LENGTH = 5
UDS_DTC_HIGH_BYTE = 0x01
UDS_DTC_DEFAULT_STATUS = 0x2F


def encode_obd_dtcs(dtcs):
    # High-performance OBD DTC encoder using direct byte manipulation and bitwise lookups
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            dtcs_bytes.append(DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]])
            dtcs_bytes.append((HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]])
    return dtcs_bytes


def encode_uds_dtcs(dtcs):
    # High-performance UDS DTC encoder using direct byte manipulation and bitwise lookups
    dtcs_bytes = bytearray()
    for dtc in dtcs:
        if is_dtc_valid(dtc):
            dtcs_bytes.append(DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]])
            dtcs_bytes.append((HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]])
            dtcs_bytes.append(UDS_DTC_HIGH_BYTE)
            dtcs_bytes.append(UDS_DTC_DEFAULT_STATUS)
    return dtcs_bytes


def is_dtc_valid(dtc):
    # Fast O(1) dictionary lookup validation bypassing try/except overhead
    return (
        len(dtc) == DTC_LENGTH
        and dtc[0] in DTC_GROUP_BITS
        and dtc[1] in DTC_TYPE_BITS
        and dtc[2] in HEX_VAL
        and dtc[3] in HEX_VAL
        and dtc[4] in HEX_VAL
    )


def get_dtc_first_byte(dtc):
    return bytes([(DTC_GROUP_BITS[dtc[0]] | DTC_TYPE_BITS[dtc[1]] | HEX_VAL[dtc[2]])])


def get_dtc_second_byte(dtc):
    return bytes([((HEX_VAL[dtc[3]] << 4) | HEX_VAL[dtc[4]])])


def is_hex_value(value):
    return value in HEX_VAL
