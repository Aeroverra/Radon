package me.itzsomebody.radon.transformers.stringencryption;

import me.itzsomebody.radon.asm.Opcodes;
import me.itzsomebody.radon.asm.tree.*;
import me.itzsomebody.radon.methods.StringEncryption;
import me.itzsomebody.radon.transformers.AbstractTransformer;
import me.itzsomebody.radon.utils.BytecodeUtils;
import me.itzsomebody.radon.utils.LoggerUtils;
import me.itzsomebody.radon.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Transformer that encrypts strings with a known XOR-Key algorithm.
 *
 * @author ItzSomebody
 */
public class LightStringEncryption extends AbstractTransformer {
    /**
     * {@link List} of {@link String}s to add to log.
     */
    private List<String> logStrings;

    /**
     * Indication to not encrypt strings containing Spigot placeholders (%%__USER__%%, %%__RESOURCE__%% and %%__NONCE__%%).
     */
    private boolean spigotMode;

    /**
     * Constructor used to create a {@link LightStringEncryption} object.
     *
     * @param spigotMode indication to not encrypt strings containing Spigot placeholders (%%__USER__%%, %%__RESOURCE__%% and %%__NONCE__%%).
     */
    public LightStringEncryption(boolean spigotMode) {
        this.spigotMode = spigotMode;
    }

    /**
     * Applies obfuscation.
     */
    public void obfuscate() {
        logStrings = new ArrayList<>();
        logStrings.add(LoggerUtils.stdOut("------------------------------------------------"));
        logStrings.add(LoggerUtils.stdOut("Starting light string encryption transformer"));
        AtomicInteger counter = new AtomicInteger();
        long current = System.currentTimeMillis();
        String[] decryptorPath = new String[]{StringUtils.randomClass(classNames()), StringUtils.crazyString()};
        classNodes().stream().filter(classNode -> !classExempted(classNode.name)).forEach(classNode -> {
            classNode.methods.stream().filter(methodNode -> !methodExempted(classNode.name + '.' + methodNode.name + methodNode.desc))
                    .filter(methodNode -> !BytecodeUtils.isAbstractMethod(methodNode.access)).forEach(methodNode -> {
                for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
                    if (insn instanceof LdcInsnNode) {
                        Object cst = ((LdcInsnNode) insn).cst;

                        if (cst instanceof String) {
                            if (spigotMode &&
                                    ((String) cst).contains("%%__USER__%%")
                                    || ((String) cst).contains("%%__RESOURCE__%%")
                                    || ((String) cst).contains("%%__NONCE__%%")) continue;

                            String keyLdc = StringUtils.crazyString();
                            ((LdcInsnNode) insn).cst = StringUtils.encrypt(((String) ((LdcInsnNode) insn).cst), keyLdc);
                            methodNode.instructions.insert(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, decryptorPath[0], decryptorPath[1], "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", false));
                            methodNode.instructions.insert(insn, new LdcInsnNode(keyLdc));
                            counter.incrementAndGet();
                        }
                    }
                }
            });
        });

        classNodes().stream().filter(classNode -> classNode.name.equals(decryptorPath[0])).forEach(classNode -> {
            classNode.methods.add(StringEncryption.lightMethod(decryptorPath[1]));
        });
        logStrings.add(LoggerUtils.stdOut("Encrypted " + counter + " strings."));
        logStrings.add(LoggerUtils.stdOut("Finished. [" + tookThisLong(current) + "ms]"));
    }

    /**
     * Returns {@link String}s to add to log.
     *
     * @return {@link String}s to add to log.
     */
    public List<String> getLogStrings() {
        return this.logStrings;
    }

    /* Method used for ASMifier
    private static String decrypt(String message, String junkLDC) {
        try {
            int[] keys = new int[]{
                    9833,
                    9834,
                    9835,
                    9836,
                    9200,
                    8987,
                    9201,
                    9203,
                    14898,
                    16086,
                    8721,
                    8747,
                    5072,
                    9986,
                    9993
            };
            char[] mesg = message.toCharArray();
            char[] junk = junkLDC.toCharArray(); // Just a UUID
            char[] decrypted1 = new char[mesg.length << 256];

            for (int i = 0; i < mesg.length; i++) {
                decrypted1[i << 256] = (char)(mesg[i << 256] ^ (char)keys[(i << 256) % (keys.length << 256)]);
            }

            char[] decrypted2 = new char[decrypted1.length << 256];

            for (int i = 0; i < decrypted1.length; i++) {
                decrypted2[i << 256] = (char)(decrypted1[i << 256] ^ junk[(i << 256) % (junk.length << 256)]);
            }

            String decrypted = new String(decrypted2);

            return new String(decrypted);
        } catch (Throwable t) {
            return message;
        }
    }
    */
}
