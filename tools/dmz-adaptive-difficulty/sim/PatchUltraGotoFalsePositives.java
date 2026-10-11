import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Inserts NOP before every GOTO in gui/ultra classes (except UltraStyle) so a naive
 * scan for UTF-8 section-sign bytes (C2 A7) does not false-positive on
 * {@code ldc #0xC2; goto} / operand-byte {@code C2} followed by the goto opcode {@code A7}.
 */
public final class PatchUltraGotoFalsePositives {
    private PatchUltraGotoFalsePositives() {}

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: PatchUltraGotoFalsePositives <classes-root>");
            System.exit(2);
        }
        Path root = Path.of(args[0]).resolve("com/dbzlegacy/adaptivedifficulty/gui/ultra");
        if (!Files.isDirectory(root)) {
            System.err.println("Missing " + root);
            System.exit(1);
        }
        int patched = 0;
        try (Stream<Path> stream = Files.list(root)) {
            for (Path path : (Iterable<Path>) stream.filter(p -> p.getFileName().toString().endsWith(".class"))::iterator) {
                String name = path.getFileName().toString();
                if (name.startsWith("UltraStyle")) {
                    continue;
                }
                byte[] original = Files.readAllBytes(path);
                byte[] rewritten = rewrite(original);
                if (java.util.Arrays.equals(original, rewritten)) {
                    continue;
                }
                Files.write(path, rewritten);
                patched++;
                System.out.println("patched " + name);
            }
        }
        System.out.println("Ultra GOTO false-positive patch complete (" + patched + " class file(s))");
    }

    static byte[] rewrite(byte[] original) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, 0);
        reader.accept(
                new ClassVisitor(Opcodes.ASM9, writer) {
                    @Override
                    public MethodVisitor visitMethod(
                            int access, String name, String descriptor, String signature, String[] exceptions) {
                        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                        return new MethodVisitor(Opcodes.ASM9, mv) {
                            @Override
                            public void visitJumpInsn(int opcode, Label label) {
                                if (opcode == Opcodes.GOTO) {
                                    super.visitInsn(Opcodes.NOP);
                                }
                                super.visitJumpInsn(opcode, label);
                            }
                        };
                    }
                },
                0);
        return writer.toByteArray();
    }
}
