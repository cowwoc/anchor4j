# ModernJDK License 1.1

**Effective Date:** 2025-06-27  
**Applies To:** Anchor4J and all associated binaries and source code

---

## 1. Purpose

The **ModernJDK License** is designed to:

- Encourage adoption of modern Java runtimes by allowing free use of Anchor4J on the latest Java versions;
- Prevent misuse of Anchor4J on outdated Java environments;
- Allow open-source projects and small commercial users (≤10 employees) to freely use Anchor4J **without
  affecting their own license**;
- Require larger commercial users to purchase a commercial license.

This license applies only to **Anchor4J itself**. Projects that depend on Anchor4J may retain their own
license (e.g., Apache 2.0, MIT, GPL, commercial), provided that:

- They do not modify or relicense Anchor4J,
- They do not re-export or wrap Anchor4J’s API in an attempt to bypass this license or its JDK version
  restrictions,
- And, if commercial, they comply with the usage terms in Section 5.

---

## 2. License Grant

You are granted a non-exclusive, non-transferable, non-sublicensable license to:

- Use Anchor4J in source or binary form,
- Include it as a dependency in your software,
- Bundle it in compiled outputs (e.g., fat JARs, containers, native images),

**provided that** your use complies with the restrictions described in Section 4 and (if applicable) Section
5.

---

## 3. Scope of This License

This license applies **solely to Anchor4J** ("the Software"), regardless of how it is obtained, embedded,
bundled, shaded, or redistributed. The terms of this license continue to apply to the Software even when:

- It is included within another project,
- It is shaded or relocated using build tools,
- It is packaged within a container or native executable.

Redistribution of Anchor4J, whether standalone or bundled, must preserve this license in full and make it
accessible to downstream users.

---

## 4. Restrictions

The following conditions apply to all use and redistribution of the Software:

1. **JDK Version Compliance**  
   The Software may only be executed using a version of the Java Development Kit (JDK) that was the latest
   generally available (GA) release at the time of the Software's original public release, or any newer
   version.  
   Use with older JDK versions is **prohibited**, unless explicitly licensed under separate commercial terms.

2. **Modification**  
   You may not modify the Software to circumvent this license or to allow execution on JDK versions not
   covered by Section 4.1.

3. **Attribution and Licensing**  
   You must retain all copyright notices, license texts, and version metadata.  
   You may not relicense or repackage the Software under any other terms.

---

## 5. Commercial Use Restriction

Commercial use of Anchor4J is permitted under this license **only** if:

- The organization using Anchor4J has **10 or fewer employees**, or
- The use occurs within an open-source project as defined by
  the [Open Source Definition](https://opensource.org/osd).

Organizations with **more than 10 employees** must obtain a separate commercial license to use Anchor4J in
production, regardless of whether it is bundled, linked, or used indirectly.

“Commercial use” includes, but is not limited to:

- Using Anchor4J in any internal or external business system,
- Offering products or services that depend on Anchor4J (whether paid or free),
- Using Anchor4J as part of consulting, integration, or deployment work.

A valid commercial license allows commercial users to retain any license of their choice for their own
software, **as long as they do not attempt to circumvent the terms of the commercial license** (e.g., via
unauthorized redistribution, backporting, or API wrapping).

---

## 6. Downstream Works

You may include Anchor4J as a dependency in your own software, whether open-source or proprietary, and your
software ("Dependent Work") may retain its own license (e.g., Apache 2.0, MIT, GPL, commercial), **provided
that**:

- The Dependent Work does not modify or redistribute Anchor4J in a way that violates this license,
- The Dependent Work is not designed or marketed primarily as a wrapper, adapter,
