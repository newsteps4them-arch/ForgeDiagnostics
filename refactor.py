import re

file_path = "app/src/test/java/com/forge/app/ObdParserAndProtocolTest.kt"
with open(file_path, "r") as f:
    content = f.read()

old_code = """        fun parseDtcResponse(hexResponse: String): List<String> {
            // Mode 03 response: "43 01 33 03 00 00 00" -> P0133, P0300
            val clean = hexResponse.replace(" ", "").trim()
            if (!clean.startsWith("43")) return emptyList()
            val dtcs = mutableListOf<String>()
            val payload = clean.substring(2)
            for (i in 0 until payload.length step 4) {
                if (i + 4 <= payload.length) {
                    val codeHex = payload.substring(i, i + 4)
                    if (codeHex == "0000") continue
                    val firstNibble = codeHex[0].digitToInt(16)
                    val prefix = when (firstNibble shr 2) {
                        0 -> "P"
                        1 -> "C"
                        2 -> "B"
                        3 -> "U"
                        else -> "P"
                    }
                    val firstCharNum = (firstNibble and 0x03).toString()
                    val restChars = codeHex.substring(1)
                    dtcs.add("$prefix$firstCharNum$restChars")
                }
            }
            return dtcs
        }"""

new_code = """        private fun parseCodeHex(codeHex: String): String? {
            if (codeHex == "0000") return null
            val firstNibble = codeHex[0].digitToInt(16)
            val prefix = when (firstNibble shr 2) {
                0 -> "P"
                1 -> "C"
                2 -> "B"
                3 -> "U"
                else -> "P"
            }
            val firstCharNum = (firstNibble and 0x03).toString()
            val restChars = codeHex.substring(1)
            return "$prefix$firstCharNum$restChars"
        }

        fun parseDtcResponse(hexResponse: String): List<String> {
            // Mode 03 response: "43 01 33 03 00 00 00" -> P0133, P0300
            val clean = hexResponse.replace(" ", "").trim()
            if (!clean.startsWith("43")) return emptyList()
            val dtcs = mutableListOf<String>()
            val payload = clean.substring(2)
            for (i in 0 until payload.length step 4) {
                if (i + 4 <= payload.length) {
                    val codeHex = payload.substring(i, i + 4)
                    parseCodeHex(codeHex)?.let { dtcs.add(it) }
                }
            }
            return dtcs
        }"""

if old_code in content:
    content = content.replace(old_code, new_code)
    with open(file_path, "w") as f:
        f.write(content)
    print("Success: Code replaced.")
else:
    print("Error: Old code block not found exactly as written.")
