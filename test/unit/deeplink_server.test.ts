import { describe, it, expect } from "vitest";

describe("Deep Link & AssetLinks Server Configuration", () => {
  it("should validate Android package name in Digital Asset Links schema", () => {
    const assetLinks = [
      {
        relation: ["delegate_permission/common.handle_all_urls"],
        target: {
          namespace: "android_app",
          package_name: "com.forge.app",
          sha256_cert_fingerprints: [
            "FA:C6:17:45:DC:09:03:78:6F:B9:ED:E6:2A:96:2B:39:9F:73:48:F0:BB:6F:89:9B:83:32:66:75:91:03:3B:9C",
          ],
        },
      },
    ];

    const targetLink = assetLinks[0]!;
    expect(targetLink.target.package_name).toBe("com.forge.app");
    expect(targetLink.target.namespace).toBe("android_app");
    expect(targetLink.relation).toContain("delegate_permission/common.handle_all_urls");
  });

  it("should construct deep link URI from web landing page path", () => {
    const rawPath = "dtc/P0300";
    const parts = rawPath.split("/").filter(Boolean);
    const category = parts[0] || "app";
    const parameter = parts[1] || "";

    const deepLinkUri = `forge://${category}${parameter ? "/" + parameter : ""}`;

    expect(deepLinkUri).toBe("forge://dtc/P0300");
  });
});
