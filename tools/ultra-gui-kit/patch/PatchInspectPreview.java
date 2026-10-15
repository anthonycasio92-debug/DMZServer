package com.dbzlegacy.kaipath.patch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Copy a stock KaiPath jar and inject {@code InspectPreview.tryRender} at the start of
 * {@code LmDynamicScreen.renderPreview}. Every other class stays byte-for-byte.
 */
public final class PatchInspectPreview {
    private static final String SCREEN =
            "com/dmzlegacy/kaipath/ultra/consumers/legacymechanics/LmDynamicScreen";
    private static final String HOOK =
            "com/dmzlegacy/kaipath/ultra/consumers/legacymechanics/InspectPreview";

    private PatchInspectPreview() {}

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            System.err.println(
                    "Usage: PatchInspectPreview <base.jar> <out.jar> <InspectPreview.class> <InspectPreviewSubjects.class>");
            System.exit(2);
        }
        Path base = Path.of(args[0]);
        Path out = Path.of(args[1]);
        Path hook = Path.of(args[2]);
        Path subjects = Path.of(args[3]);
        byte[] patched = patchScreen(entry(base, SCREEN + ".class"));
        if (!new String(patched, java.nio.charset.StandardCharsets.ISO_8859_1).contains("tryRender")) {
            throw new IllegalStateException("patched renderPreview is missing tryRender");
        }
        writeJar(base, out, patched, Files.readAllBytes(hook), Files.readAllBytes(subjects));
        System.out.println("Wrote " + out);
    }

    static byte[] patchScreen(byte[] original) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer =
                new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES) {
                    @Override
                    protected String getCommonSuperClass(String type1, String type2) {
                        return "java/lang/Object";
                    }
                };
        ClassVisitor visitor =
                new ClassVisitor(Opcodes.ASM9, writer) {
                    @Override
                    public MethodVisitor visitMethod(
                            int access,
                            String name,
                            String descriptor,
                            String signature,
                            String[] exceptions) {
                        MethodVisitor parent =
                                super.visitMethod(access, name, descriptor, signature, exceptions);
                        if (!"renderPreview".equals(name)
                                || !"(Lnet/minecraft/client/gui/GuiGraphics;)V".equals(descriptor)) {
                            return parent;
                        }
                        return new MethodVisitor(Opcodes.ASM9, parent) {
                            private boolean injected;

                            @Override
                            public void visitCode() {
                                super.visitCode();
                                if (injected) {
                                    return;
                                }
                                injected = true;
                                super.visitVarInsn(Opcodes.ALOAD, 0);
                                super.visitVarInsn(Opcodes.ALOAD, 1);
                                super.visitMethodInsn(
                                        Opcodes.INVOKESTATIC,
                                        HOOK,
                                        "tryRender",
                                        "(L" + SCREEN + ";Lnet/minecraft/client/gui/GuiGraphics;)Z",
                                        false);
                                Label cont = new Label();
                                super.visitJumpInsn(Opcodes.IFEQ, cont);
                                super.visitInsn(Opcodes.RETURN);
                                super.visitLabel(cont);
                            }
                        };
                    }
                };
        reader.accept(visitor, ClassReader.SKIP_FRAMES);
        return writer.toByteArray();
    }

    private static byte[] entry(Path jar, String name) throws IOException {
        try (JarFile file = new JarFile(jar.toFile())) {
            JarEntry found = file.getJarEntry(name);
            if (found == null) {
                throw new IllegalStateException("missing " + name + " in " + jar);
            }
            try (InputStream in = file.getInputStream(found)) {
                return in.readAllBytes();
            }
        }
    }

    private static void writeJar(Path base, Path out, byte[] screen, byte[] hook, byte[] subjects)
            throws IOException {
        Files.createDirectories(out.getParent() == null ? Path.of(".") : out.getParent());
        try (JarFile file = new JarFile(base.toFile());
                OutputStream raw = Files.newOutputStream(out);
                JarOutputStream jar = new JarOutputStream(raw)) {
            var entries = file.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.equals(HOOK + ".class") || name.equals(HOOK.replace("InspectPreview", "InspectPreviewSubjects") + ".class")) {
                    continue;
                }
                JarEntry copy = new JarEntry(name);
                jar.putNextEntry(copy);
                if (name.equals(SCREEN + ".class")) {
                    jar.write(screen);
                } else if (!entry.isDirectory()) {
                    try (InputStream in = file.getInputStream(entry)) {
                        in.transferTo(jar);
                    }
                }
                jar.closeEntry();
            }
        }
        addClass(out, HOOK + ".class", hook);
        addClass(out, "com/dmzlegacy/kaipath/ultra/consumers/legacymechanics/InspectPreviewSubjects.class", subjects);
    }

    private static void addClass(Path jar, String name, byte[] bytes) throws IOException {
        Path tmp = jar.resolveSibling(jar.getFileName() + ".tmp");
        try (JarFile file = new JarFile(jar.toFile());
                OutputStream raw = Files.newOutputStream(tmp);
                JarOutputStream out = new JarOutputStream(raw)) {
            var entries = file.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().equals(name)) {
                    continue;
                }
                out.putNextEntry(new JarEntry(entry.getName()));
                if (!entry.isDirectory()) {
                    try (InputStream in = file.getInputStream(entry)) {
                        in.transferTo(out);
                    }
                }
                out.closeEntry();
            }
            out.putNextEntry(new JarEntry(name));
            out.write(bytes);
            out.closeEntry();
        }
        Files.move(tmp, jar, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
}
