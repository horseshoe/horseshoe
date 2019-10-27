package horseshoe.internal;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MethodBuilder<T> {

	private static class StringReference {
		private final String value;

		public StringReference(final String value) {
			this.value = value;
		}

		@Override
		public boolean equals(final Object obj) {
			return obj instanceof StringReference && value.equals(((StringReference)obj).value);
		}

		@Override
		public int hashCode() {
			return StringReference.class.hashCode() ^ value.hashCode();
		}
	}

	public static final byte AALOAD = (byte)0x32; // (stack: arrayref, index -> value)
	public static final byte AASTORE = (byte)0x53; // (stack: arrayref, index, value ->)
	public static final byte ACONST_NULL = (byte)0x01; // (stack: -> null)
	public static final byte ALOAD = (byte)0x19; // 1: index (stack: -> objectref)
	public static final byte ALOAD_0 = (byte)0x2A; // (stack: -> objectref)
	public static final byte ALOAD_1 = (byte)0x2B; // (stack: -> objectref)
	public static final byte ALOAD_2 = (byte)0x2C; // (stack: -> objectref)
	public static final byte ALOAD_3 = (byte)0x2D; // (stack: -> objectref)
	public static final byte ANEWARRAY = (byte)0xBD; // 2: indexbyte1, indexbyte2 (stack: count -> arrayref)
	public static final byte ARETURN = (byte)0xB0; // (stack: objectref -> [empty])
	public static final byte ARRAYLENGTH = (byte)0xBE; // (stack: arrayref -> length)
	public static final byte ASTORE = (byte)0x3A; // 1: index (stack: objectref ->)
	public static final byte ASTORE_0 = (byte)0x4B; // (stack: objectref ->)
	public static final byte ASTORE_1 = (byte)0x4C; // (stack: objectref ->)
	public static final byte ASTORE_2 = (byte)0x4D; // (stack: objectref ->)
	public static final byte ASTORE_3 = (byte)0x4E; // (stack: objectref ->)
	public static final byte ATHROW = (byte)0xBF; // (stack: objectref -> [empty], objectref)
	public static final byte BALOAD = (byte)0x33; // (stack: arrayref, index -> value)
	public static final byte BASTORE = (byte)0x54; // (stack: arrayref, index, value ->)
	public static final byte BIPUSH = (byte)0x10; // 1: byte (stack: -> value)
	public static final byte BREAKPOINT = (byte)0xCA; // (stack: )
	public static final byte CALOAD = (byte)0x34; // (stack: arrayref, index -> value)
	public static final byte CASTORE = (byte)0x55; // (stack: arrayref, index, value ->)
	public static final byte CHECKCAST = (byte)0xC0; // 2: indexbyte1, indexbyte2 (stack: objectref -> objectref)
	public static final byte D2F = (byte)0x90; // (stack: value -> result)
	public static final byte D2I = (byte)0x8E; // (stack: value -> result)
	public static final byte D2L = (byte)0x8F; // (stack: value -> result)
	public static final byte DADD = (byte)0x63; // (stack: value1, value2 -> result)
	public static final byte DALOAD = (byte)0x31; // (stack: arrayref, index -> value)
	public static final byte DASTORE = (byte)0x52; // (stack: arrayref, index, value ->)
	public static final byte DCMPG = (byte)0x98; // (stack: value1, value2 -> result)
	public static final byte DCMPL = (byte)0x97; // (stack: value1, value2 -> result)
	public static final byte DCONST_0 = (byte)0x0E; // (stack: -> 0.0)
	public static final byte DCONST_1 = (byte)0x0F; // (stack: -> 1.0)
	public static final byte DDIV = (byte)0x6F; // (stack: value1, value2 -> result)
	public static final byte DLOAD = (byte)0x18; // 1: index (stack: -> value)
	public static final byte DLOAD_0 = (byte)0x26; // (stack: -> value)
	public static final byte DLOAD_1 = (byte)0x27; // (stack: -> value)
	public static final byte DLOAD_2 = (byte)0x28; // (stack: -> value)
	public static final byte DLOAD_3 = (byte)0x29; // (stack: -> value)
	public static final byte DMUL = (byte)0x6B; // (stack: value1, value2 -> result)
	public static final byte DNEG = (byte)0x77; // (stack: value -> result)
	public static final byte DREM = (byte)0x73; // (stack: value1, value2 -> result)
	public static final byte DRETURN = (byte)0xAF; // (stack: value -> [empty])
	public static final byte DSTORE = (byte)0x39; // 1: index (stack: value ->)
	public static final byte DSTORE_0 = (byte)0x47; // (stack: value ->)
	public static final byte DSTORE_1 = (byte)0x48; // (stack: value ->)
	public static final byte DSTORE_2 = (byte)0x49; // (stack: value ->)
	public static final byte DSTORE_3 = (byte)0x4A; // (stack: value ->)
	public static final byte DSUB = (byte)0x67; // (stack: value1, value2 -> result)
	public static final byte DUP = (byte)0x59; // (stack: value -> value, value)
	public static final byte DUP_X1 = (byte)0x5A; // (stack: value2, value1 -> value1, value2, value1)
	public static final byte DUP_X2 = (byte)0x5B; // (stack: value3, value2, value1 -> value1, value3, value2, value1)
	public static final byte DUP2 = (byte)0x5C; // (stack: {value2, value1} -> {value2, value1}, {value2, value1})
	public static final byte DUP2_X1 = (byte)0x5D; // (stack: value3, {value2, value1} -> {value2, value1}, value3, {value2, value1})
	public static final byte DUP2_X2 = (byte)0x5E; // (stack: {value4, value3}, {value2, value1} -> {value2, value1}, {value4, value3}, {value2, value1})
	public static final byte F2D = (byte)0x8D; // (stack: value -> result)
	public static final byte F2I = (byte)0x8B; // (stack: value -> result)
	public static final byte F2L = (byte)0x8C; // (stack: value -> result)
	public static final byte FADD = (byte)0x62; // (stack: value1, value2 -> result)
	public static final byte FALOAD = (byte)0x30; // (stack: arrayref, index -> value)
	public static final byte FASTORE = (byte)0x51; // (stack: arrayref, index, value ->)
	public static final byte FCMPG = (byte)0x96; // (stack: value1, value2 -> result)
	public static final byte FCMPL = (byte)0x95; // (stack: value1, value2 -> result)
	public static final byte FCONST_0 = (byte)0x0B; // (stack: -> 0.0f)
	public static final byte FCONST_1 = (byte)0x0C; // (stack: -> 1.0f)
	public static final byte FCONST_2 = (byte)0x0D; // (stack: -> 2.0f)
	public static final byte FDIV = (byte)0x6E; // (stack: value1, value2 -> result)
	public static final byte FLOAD = (byte)0x17; // 1: index (stack: -> value)
	public static final byte FLOAD_0 = (byte)0x22; // (stack: -> value)
	public static final byte FLOAD_1 = (byte)0x23; // (stack: -> value)
	public static final byte FLOAD_2 = (byte)0x24; // (stack: -> value)
	public static final byte FLOAD_3 = (byte)0x25; // (stack: -> value)
	public static final byte FMUL = (byte)0x6A; // (stack: value1, value2 -> result)
	public static final byte FNEG = (byte)0x76; // (stack: value -> result)
	public static final byte FREM = (byte)0x72; // (stack: value1, value2 -> result)
	public static final byte FRETURN = (byte)0xAE; // (stack: value -> [empty])
	public static final byte FSTORE = (byte)0x38; // 1: index (stack: value ->)
	public static final byte FSTORE_0 = (byte)0x43; // (stack: value ->)
	public static final byte FSTORE_1 = (byte)0x44; // (stack: value ->)
	public static final byte FSTORE_2 = (byte)0x45; // (stack: value ->)
	public static final byte FSTORE_3 = (byte)0x46; // (stack: value ->)
	public static final byte FSUB = (byte)0x66; // (stack: value1, value2 -> result)
	public static final byte GETFIELD = (byte)0xB4; // 2: indexbyte1, indexbyte2 (stack: objectref -> value)
	public static final byte GETSTATIC = (byte)0xB2; // 2: indexbyte1, indexbyte2 (stack: -> value)
	public static final byte GOTO = (byte)0xA7; // 2: branchbyte1, branchbyte2 (stack: [no change])
	public static final byte GOTO_W = (byte)0xC8; // 4: branchbyte1, branchbyte2, branchbyte3, branchbyte4 (stack: [no change])
	public static final byte I2B = (byte)0x91; // (stack: value -> result)
	public static final byte I2C = (byte)0x92; // (stack: value -> result)
	public static final byte I2D = (byte)0x87; // (stack: value -> result)
	public static final byte I2F = (byte)0x86; // (stack: value -> result)
	public static final byte I2L = (byte)0x85; // (stack: value -> result)
	public static final byte I2S = (byte)0x93; // (stack: value -> result)
	public static final byte IADD = (byte)0x60; // (stack: value1, value2 -> result)
	public static final byte IALOAD = (byte)0x2E; // (stack: arrayref, index -> value)
	public static final byte IAND = (byte)0x7E; // (stack: value1, value2 -> result)
	public static final byte IASTORE = (byte)0x4F; // (stack: arrayref, index, value ->)
	public static final byte ICONST_M1 = (byte)0x02; // (stack: -> -1)
	public static final byte ICONST_0 = (byte)0x03; // (stack: -> 0)
	public static final byte ICONST_1 = (byte)0x04; // (stack: -> 1)
	public static final byte ICONST_2 = (byte)0x05; // (stack: -> 2)
	public static final byte ICONST_3 = (byte)0x06; // (stack: -> 3)
	public static final byte ICONST_4 = (byte)0x07; // (stack: -> 4)
	public static final byte ICONST_5 = (byte)0x08; // (stack: -> 5)
	public static final byte IDIV = (byte)0x6C; // (stack: value1, value2 -> result)
	public static final byte IF_ACMPEQ = (byte)0xA5; // 2: branchbyte1, branchbyte2 (stack: value1, value2 ->)
	public static final byte IF_ACMPNE = (byte)0xA6; // 2: branchbyte1, branchbyte2 (stack: value1, value2 ->)
	public static final byte IF_ICMPEQ = (byte)0x9F; // 2: branchbyte1, branchbyte2 (stack: value1, value2 ->)
	public static final byte IF_ICMPGE = (byte)0xA2; // 2: branchbyte1, branchbyte2 (stack: value1, value2 ->)
	public static final byte IF_ICMPGT = (byte)0xA3; // 2: branchbyte1, branchbyte2 (stack: value1, value2 ->)
	public static final byte IF_ICMPLE = (byte)0xA4; // 2: branchbyte1, branchbyte2 (stack: value1, value2 ->)
	public static final byte IF_ICMPLT = (byte)0xA1; // 2: branchbyte1, branchbyte2 (stack: value1, value2 ->)
	public static final byte IF_ICMPNE = (byte)0xA0; // 2: branchbyte1, branchbyte2 (stack: value1, value2 ->)
	public static final byte IFEQ = (byte)0x99; // 2: branchbyte1, branchbyte2 (stack: value ->)
	public static final byte IFGE = (byte)0x9C; // 2: branchbyte1, branchbyte2 (stack: value ->)
	public static final byte IFGT = (byte)0x9D; // 2: branchbyte1, branchbyte2 (stack: value ->)
	public static final byte IFLE = (byte)0x9E; // 2: branchbyte1, branchbyte2 (stack: value ->)
	public static final byte IFLT = (byte)0x9B; // 2: branchbyte1, branchbyte2 (stack: value ->)
	public static final byte IFNE = (byte)0x9A; // 2: branchbyte1, branchbyte2 (stack: value ->)
	public static final byte IFNONNULL = (byte)0xC7; // 2: branchbyte1, branchbyte2 (stack: value ->)
	public static final byte IFNULL = (byte)0xC6; // 2: branchbyte1, branchbyte2 (stack: value ->)
	public static final byte IINC = (byte)0x84; // 2: index, const (stack: [No change])
	public static final byte ILOAD = (byte)0x15; // 1: index (stack: -> value)
	public static final byte ILOAD_0 = (byte)0x1A; // (stack: -> value)
	public static final byte ILOAD_1 = (byte)0x1B; // (stack: -> value)
	public static final byte ILOAD_2 = (byte)0x1C; // (stack: -> value)
	public static final byte ILOAD_3 = (byte)0x1D; // (stack: -> value)
	public static final byte IMPDEP1 = (byte)0xFE; // (stack: )
	public static final byte IMPDEP2 = (byte)0xFF; // (stack: )
	public static final byte IMUL = (byte)0x68; // (stack: value1, value2 -> result)
	public static final byte INEG = (byte)0x74; // (stack: value -> result)
	public static final byte INSTANCEOF = (byte)0xC1; // 2: indexbyte1, indexbyte2 (stack: objectref -> result)
	public static final byte INVOKEDYNAMIC = (byte)0xBA; // 4: indexbyte1, indexbyte2, 0, 0 (stack: [arg1, [arg2 ...]] -> result)
	public static final byte INVOKEINTERFACE = (byte)0xB9; // 4: indexbyte1, indexbyte2, count, 0 (stack: objectref, [arg1, arg2, ...] -> result)
	public static final byte INVOKESPECIAL = (byte)0xB7; // 2: indexbyte1, indexbyte2 (stack: objectref, [arg1, arg2, ...] -> result)
	public static final byte INVOKESTATIC = (byte)0xB8; // 2: indexbyte1, indexbyte2 (stack: [arg1, arg2, ...] -> result)
	public static final byte INVOKEVIRTUAL = (byte)0xB6; // 2: indexbyte1, indexbyte2 (stack: objectref, [arg1, arg2, ...] -> result)
	public static final byte IOR = (byte)0x80; // (stack: value1, value2 -> result)
	public static final byte IREM = (byte)0x70; // (stack: value1, value2 -> result)
	public static final byte IRETURN = (byte)0xAC; // (stack: value -> [empty])
	public static final byte ISHL = (byte)0x78; // (stack: value1, value2 -> result)
	public static final byte ISHR = (byte)0x7A; // (stack: value1, value2 -> result)
	public static final byte ISTORE = (byte)0x36; // 1: index (stack: value ->)
	public static final byte ISTORE_0 = (byte)0x3B; // (stack: value ->)
	public static final byte ISTORE_1 = (byte)0x3C; // (stack: value ->)
	public static final byte ISTORE_2 = (byte)0x3D; // (stack: value ->)
	public static final byte ISTORE_3 = (byte)0x3E; // (stack: value ->)
	public static final byte ISUB = (byte)0x64; // (stack: value1, value2 -> result)
	public static final byte IUSHR = (byte)0x7C; // (stack: value1, value2 -> result)
	public static final byte IXOR = (byte)0x82; // (stack: value1, value2 -> result)
	public static final byte JSR = (byte)0xA8; // 2: branchbyte1, branchbyte2 (stack: -> address)
	public static final byte JSR_W = (byte)0xC9; // 4: branchbyte1, branchbyte2, branchbyte3, branchbyte4 (stack: -> address)
	public static final byte L2D = (byte)0x8A; // (stack: value -> result)
	public static final byte L2F = (byte)0x89; // (stack: value -> result)
	public static final byte L2I = (byte)0x88; // (stack: value -> result)
	public static final byte LADD = (byte)0x61; // (stack: value1, value2 -> result)
	public static final byte LALOAD = (byte)0x2F; // (stack: arrayref, index -> value)
	public static final byte LAND = (byte)0x7F; // (stack: value1, value2 -> result)
	public static final byte LASTORE = (byte)0x50; // (stack: arrayref, index, value ->)
	public static final byte LCMP = (byte)0x94; // (stack: value1, value2 -> result)
	public static final byte LCONST_0 = (byte)0x09; // (stack: -> 0L)
	public static final byte LCONST_1 = (byte)0x0A; // (stack: -> 1L)
	public static final byte LDC = (byte)0x12; // 1: index (stack: -> value)
	public static final byte LDC_W = (byte)0x13; // 2: indexbyte1, indexbyte2 (stack: -> value)
	public static final byte LDC2_W = (byte)0x14; // 2: indexbyte1, indexbyte2 (stack: -> value)
	public static final byte LDIV = (byte)0x6D; // (stack: value1, value2 -> result)
	public static final byte LLOAD = (byte)0x16; // 1: index (stack: -> value)
	public static final byte LLOAD_0 = (byte)0x1E; // (stack: -> value)
	public static final byte LLOAD_1 = (byte)0x1F; // (stack: -> value)
	public static final byte LLOAD_2 = (byte)0x20; // (stack: -> value)
	public static final byte LLOAD_3 = (byte)0x21; // (stack: -> value)
	public static final byte LMUL = (byte)0x69; // (stack: value1, value2 -> result)
	public static final byte LNEG = (byte)0x75; // (stack: value -> result)
	public static final byte LOOKUPSWITCH = (byte)0xAB; // 8+: <0–3 bytes padding>, defaultbyte1, defaultbyte2, defaultbyte3, defaultbyte4, npairs1, npairs2, npairs3, npairs4, match-offset pairs... (stack: key ->)
	public static final byte LOR = (byte)0x81; // (stack: value1, value2 -> result)
	public static final byte LREM = (byte)0x71; // (stack: value1, value2 -> result)
	public static final byte LRETURN = (byte)0xAD; // (stack: value -> [empty])
	public static final byte LSHL = (byte)0x79; // (stack: value1, value2 -> result)
	public static final byte LSHR = (byte)0x7B; // (stack: value1, value2 -> result)
	public static final byte LSTORE = (byte)0x37; // 1: index (stack: value ->)
	public static final byte LSTORE_0 = (byte)0x3F; // (stack: value ->)
	public static final byte LSTORE_1 = (byte)0x40; // (stack: value ->)
	public static final byte LSTORE_2 = (byte)0x41; // (stack: value ->)
	public static final byte LSTORE_3 = (byte)0x42; // (stack: value ->)
	public static final byte LSUB = (byte)0x65; // (stack: value1, value2 -> result)
	public static final byte LUSHR = (byte)0x7D; // (stack: value1, value2 -> result)
	public static final byte LXOR = (byte)0x83; // (stack: value1, value2 -> result)
	public static final byte MONITORENTER = (byte)0xC2; // (stack: objectref ->)
	public static final byte MONITOREXIT = (byte)0xC3; // (stack: objectref ->)
	public static final byte MULTIANEWARRAY = (byte)0xC5; // 3: indexbyte1, indexbyte2, dimensions (stack: count1, [count2,...] -> arrayref)
	public static final byte NEW = (byte)0xBB; // 2: indexbyte1, indexbyte2 (stack: -> objectref)
	public static final byte NEWARRAY = (byte)0xBC; // 1: atype (stack: count -> arrayref)
	public static final byte NOP = (byte)0x00; // (stack: [No change])
	public static final byte POP = (byte)0x57; // (stack: value ->)
	public static final byte POP2 = (byte)0x58; // (stack: {value2, value1} ->)
	public static final byte PUTFIELD = (byte)0xB5; // 2: indexbyte1, indexbyte2 (stack: objectref, value ->)
	public static final byte PUTSTATIC = (byte)0xB3; // 2: indexbyte1, indexbyte2 (stack: value ->)
	public static final byte RET = (byte)0xA9; // 1: index (stack: [No change])
	public static final byte RETURN = (byte)0xB1; // (stack: -> [empty])
	public static final byte SALOAD = (byte)0x35; // (stack: arrayref, index -> value)
	public static final byte SASTORE = (byte)0x56; // (stack: arrayref, index, value ->)
	public static final byte SIPUSH = (byte)0x11; // 2: byte1, byte2 (stack: -> value)
	public static final byte SWAP = (byte)0x5F; // (stack: value2, value1 -> value1, value2)
	public static final byte TABLESWITCH = (byte)0xAA; // 16+: [0–3 bytes padding], defaultbyte1, defaultbyte2, defaultbyte3, defaultbyte4, lowbyte1, lowbyte2, lowbyte3, lowbyte4, highbyte1, highbyte2, highbyte3, highbyte4, jump offsets... (stack: index ->)
	public static final byte WIDE = (byte)0xC4; // 3/5: opcode, indexbyte1, indexbyte2 (stack: [same as for corresponding instructions])

	private static final Method DEFINE_CLASS_METHOD;

	static {
		Method defineClass = null;

		try {
			defineClass = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
		} catch (final NoSuchMethodException e) {
		}

		DEFINE_CLASS_METHOD = defineClass;
		DEFINE_CLASS_METHOD.setAccessible(true);
	}

	/**
	 * Gets the string signature of a member (constructor or method).
	 *
	 * @param member the member used to create the signature
	 * @return the signature of the member
	 */
	private static String getSignature(final Member member) {
		if (member instanceof Field) {
			return Array.newInstance(((Field)member).getType(), 0).getClass().getName().substring(1);
		}

		final StringBuilder sb = new StringBuilder("(");
		final Class<?>[] parameters = (member instanceof Method ? ((Method)member).getParameterTypes() :
			member instanceof Constructor ? ((Constructor<?>)member).getParameterTypes() : new Class<?>[0]);
		final Class<?> returnType = (member instanceof Method ? ((Method)member).getReturnType() : void.class);

		for (final Class<?> type : parameters) {
			sb.append(Array.newInstance(type, 0).getClass().getName().substring(1));
		}

		if (returnType == void.class) {
			sb.append(")V");
		} else {
			sb.append(")").append(Array.newInstance(returnType, 0).getClass().getName().substring(1));
		}

		return sb.toString().replace('.', '/');
	}

	private final String name;
	private byte[] buffer = new byte[4096];
	private int length = 0;
	private final Method method;
	private final Map<Object, Short> constants = new LinkedHashMap<>();
	private final short classNameI;
	private final short baseClassI;
	private final short interfaceClassI;

	/**
	 * Creates a new method builder based off the specified interface class. The interface must only have 1 method.
	 *
	 * @param name the name of the class to build
	 * @param interfaceClass the interface class the class being built will implement
	 */
	public MethodBuilder(final String name, final Class<T> interfaceClass) {
		if (interfaceClass.getMethods().length != 1) {
			throw new RuntimeException("Interface " + interfaceClass.getCanonicalName() + " must have exactly 1 method (contains " + interfaceClass.getMethods().length + ")");
		}

		this.name = name;
		this.method = interfaceClass.getMethods()[0];

		// Magic number
		buffer[length++] = (byte)0xCA;
		buffer[length++] = (byte)0xFE;
		buffer[length++] = (byte)0xBA;
		buffer[length++] = (byte)0xBE;

		// Version number
		buffer[length++] = 0x00;
		buffer[length++] = 0x00;
		buffer[length++] = 0x00;
		buffer[length++] = 0x33;

		// Constants count (fill with zeros for now)
		buffer[length++] = 0x00;
		buffer[length++] = 0x00;

		// Add this class
		final short nameI = addString(name.replace('.', '/'));
		final int i = reserve(3);

		buffer[i] = 0x07;
		buffer[i + 1] = (byte)(nameI >>> 8);
		buffer[i + 2] = (byte)(nameI);

		final short index = (short)(constants.size() + 1);
		constants.put(this, index);
		classNameI = index;

		baseClassI = addConstant(Object.class);
		interfaceClassI = addConstant(interfaceClass);
	}

	/**
	 * Adds the specified class to the constant pool. This returns the existing index if the class already exists in the constant pool.
	 *
	 * @param value the class to add to the constant pool
	 * @return the index into the constant pool for the class
	 */
	public short addConstant(final Class<?> value) {
		final Short existing = constants.get(value);

		if (existing != null) {
			return existing;
		}

		final short nameI = addString(value.getName().replace('.', '/'));
		final int i = reserve(3);

		buffer[i] = 0x07;
		buffer[i + 1] = (byte)(nameI >>> 8);
		buffer[i + 2] = (byte)(nameI);

		final short index = (short)(constants.size() + 1);
		constants.put(value, index);
		return index;
	}

	/**
	 * Adds the specified member to the constant pool. This returns the existing index if the member already exists in the constant pool.
	 *
	 * @param value the member to add to the constant pool
	 * @return the index into the constant pool for the member
	 */
	public short addConstant(final Member value) {
		final Short existing = constants.get(value);

		if (existing != null) {
			return existing;
		}

		final Class<?> parentClass = (value instanceof Method ? ((Method)value).getDeclaringClass() :
			value instanceof Constructor ? ((Constructor<?>)value).getDeclaringClass() :
			value instanceof Field ? ((Field)value).getDeclaringClass() : null);

		final short classI = addConstant(parentClass);
		final short nameI = addString(value instanceof Constructor ? "<init>" : value.getName());
		final short typeI = addString(getSignature(value));

		// Build the signature
		final short nameAndTypeI = (short)(constants.size() + 1);
		final int i = reserve(10);

		buffer[i] = 0x0C;
		buffer[i + 1] = (byte)(nameI >>> 8);
		buffer[i + 2] = (byte)(nameI);
		buffer[i + 3] = (byte)(typeI >>> 8);
		buffer[i + 4] = (byte)(typeI);

		buffer[i + 5] = (byte)(value instanceof Field ? 0x09 : (parentClass.getModifiers() & Modifier.INTERFACE) == 0 ? 0x0A : 0x0B);
		buffer[i + 6] = (byte)(classI >>> 8);
		buffer[i + 7] = (byte)(classI);
		buffer[i + 8] = (byte)(nameAndTypeI >>> 8);
		buffer[i + 9] = (byte)(nameAndTypeI);

		final short index = (short)(constants.size() + 2);
		constants.put(new Object(), nameAndTypeI);
		constants.put(value, index);
		return index;
	}

	/**
	 * Adds the specified long to the constant pool. This returns the existing index if the long already exists in the constant pool.
	 *
	 * @param value the long to add to the constant pool
	 * @return the index into the constant pool for the long
	 */
	public short addConstant(final long value) {
		final Short existing = constants.get(value);

		if (existing != null) {
			return existing;
		}

		final int i = reserve(9);

		buffer[i] = 0x05;
		buffer[i + 1] = (byte)(value >>> 56);
		buffer[i + 2] = (byte)(value >>> 48);
		buffer[i + 3] = (byte)(value >>> 40);
		buffer[i + 4] = (byte)(value >>> 32);
		buffer[i + 5] = (byte)(value >>> 24);
		buffer[i + 6] = (byte)(value >>> 16);
		buffer[i + 7] = (byte)(value >>> 8);
		buffer[i + 8] = (byte)(value);

		final short index = (short)(constants.size() + 1);
		constants.put(value, index);
		return index;
	}

	/**
	 * Adds the specified double to the constant pool. This returns the existing index if the double already exists in the constant pool.
	 *
	 * @param value the double to add to the constant pool
	 * @return the index into the constant pool for the double
	 */
	public short addConstant(final double value) {
		final Short existing = constants.get(value);

		if (existing != null) {
			return existing;
		}

		final int i = reserve(9);
		final long longValue = Double.doubleToRawLongBits(value);

		buffer[i] = 0x06;
		buffer[i + 1] = (byte)(longValue >>> 56);
		buffer[i + 2] = (byte)(longValue >>> 48);
		buffer[i + 3] = (byte)(longValue >>> 40);
		buffer[i + 4] = (byte)(longValue >>> 32);
		buffer[i + 5] = (byte)(longValue >>> 24);
		buffer[i + 6] = (byte)(longValue >>> 16);
		buffer[i + 7] = (byte)(longValue >>> 8);
		buffer[i + 8] = (byte)(longValue);

		final short index = (short)(constants.size() + 1);
		constants.put(value, index);
		return index;
	}

	/**
	 * Adds the specified string to the constant pool. This returns the existing index if the string already exists in the constant pool.
	 *
	 * @param value the string to add to the constant pool
	 * @return the index into the constant pool for the string
	 */
	public short addConstant(final String value) {
		final StringReference reference = new StringReference(value);
		final Short existing = constants.get(reference);

		if (existing != null) {
			return existing;
		}

		final short stringI = addString(value);
		final int i = reserve(3);

		buffer[i] = 0x08;
		buffer[i + 1] = (byte)(stringI >>> 8);
		buffer[i + 2] = (byte)(stringI);

		final short index = (short)(constants.size() + 1);
		constants.put(reference, index);
		return index;
	}

	/**
	 * Adds the specified raw string to the constant pool. This returns the existing index if the string already exists in the constant pool.
	 *
	 * @param value the string to add to the constant pool
	 * @return the index into the constant pool for the string
	 */
	private short addString(final String value) {
		final Short existing = constants.get(value);

		if (existing != null) {
			return existing;
		}

		final byte[] utfChars = value.getBytes(StandardCharsets.UTF_8); // Note no length or invalid character checks
		final int i = reserve(3 + utfChars.length);

		buffer[i] = 0x01;
		buffer[i + 1] = (byte)(utfChars.length >>> 8);
		buffer[i + 2] = (byte)(utfChars.length);
		System.arraycopy(utfChars, 0, buffer, i + 3, utfChars.length);

		final short index = (short)(constants.size() + 1);
		constants.put(value, index);
		return index;
	}

	/**
	 * Reserves the specified number of bytes in the buffer. The length is automatically extended and the returned index should be used to put data in the buffer.
	 *
	 * @param size the number of bytes to reserve
	 * @return the index to use for loading data
	 */
	private int reserve(final int size) {
		if (length + size > buffer.length) {
			final byte[] newBuffer = new byte[Math.max(length + size, buffer.length * 2)];
			System.arraycopy(buffer, 0, newBuffer, 0, length);
			buffer = newBuffer;
		}

		final int index = length;
		length += size;

		return index;
	}

	/**
	 * Loads the specified method bytecode into the interface method and returns the new class.
	 *
	 * @param loader the loader to use for loading the new class
	 * @param bytecode the bytecode of the method
	 * @param end the ending index (exclusive) of the bytecode to load
	 * @param stackSize the maximum size of the operand stack in the bytecode
	 * @param variableSize the maximum size of the local variables array in the bytecode
	 * @return the class with the bytecode loaded
	 * @throws IllegalAccessException if the loader cannot load the bytecode due to the current security setup
	 * @throws InvocationTargetException if the loader throws an exception while loading the bytecode
	 */
	@SuppressWarnings("unchecked")
	public Class<T> load(final ClassLoader loader, final byte[] bytecode, final int end, final short stackSize, final short variableSize) throws IllegalAccessException, InvocationTargetException {
		final int SUPER = 0x20; // Internal flag

		final short ctorNameI = addString("<init>");
		final short ctorSignatureI = addString("()V");
		final short codeAttributeI = addString("Code");
		short baseClassCtorI = 0;

		try {
			baseClassCtorI = addConstant(Object.class.getConstructor());
		} catch (final NoSuchMethodException e) {
			// Something seriously wrong occurred
		}

		final short methodNameI = addString(method.getName());
		final short methodSignatureI = addString(getSignature(method));

		final int ctorIndex = length + 14; // The offset of the constructor start
		final int methodIndex = ctorIndex + 31; // The offset of the method start
		final int attributeIndex = methodIndex + end + 26;
		final int endIndex = attributeIndex + 2;

		// Copy the bytecode and update the constants table count
		final byte[] classBytecode = new byte[endIndex];

		System.arraycopy(buffer, 0, classBytecode, 0, length);
		classBytecode[8] = (byte)((constants.size() + 1) >>> 8);
		classBytecode[9] = (byte)((constants.size() + 1));

		// Access flags (public final super)
		final int accessFlags = Modifier.PUBLIC | Modifier.FINAL | SUPER;
		classBytecode[length]     = (byte)(accessFlags >>> 8);
		classBytecode[length + 1] = (byte)(accessFlags);

		// This class and super class index
		classBytecode[length + 2] = (byte)(classNameI >>> 8);
		classBytecode[length + 3] = (byte)(classNameI);
		classBytecode[length + 4] = (byte)(baseClassI >>> 8);
		classBytecode[length + 5] = (byte)(baseClassI);

		// Add the interface we are implementing
		classBytecode[length + 6] = 0x00;
		classBytecode[length + 7] = 0x01;
		classBytecode[length + 8] = (byte)(interfaceClassI >>> 8);
		classBytecode[length + 9] = (byte)(interfaceClassI);

		// No fields
		classBytecode[length + 10] = 0x00;
		classBytecode[length + 11] = 0x00;

		// Methods - constructor and interface method
		classBytecode[length + 12] = 0x00;
		classBytecode[length + 13] = 0x02;

		{ // Constructor
			final int ctorAccessFlags = Modifier.PUBLIC;

			classBytecode[ctorIndex]     = (byte)(ctorAccessFlags >>> 8);
			classBytecode[ctorIndex + 1] = (byte)(ctorAccessFlags);

			classBytecode[ctorIndex + 2] = (byte)(ctorNameI >>> 8);
			classBytecode[ctorIndex + 3] = (byte)(ctorNameI);
			classBytecode[ctorIndex + 4] = (byte)(ctorSignatureI >>> 8);
			classBytecode[ctorIndex + 5] = (byte)(ctorSignatureI);

			classBytecode[ctorIndex + 6] = 0x00;
			classBytecode[ctorIndex + 7] = 0x01;
			classBytecode[ctorIndex + 8] = (byte)(codeAttributeI >>> 8);
			classBytecode[ctorIndex + 9] = (byte)(codeAttributeI);

			classBytecode[ctorIndex + 10] = 0x00;
			classBytecode[ctorIndex + 11] = 0x00;
			classBytecode[ctorIndex + 12] = 0x00;
			classBytecode[ctorIndex + 13] = 0x11;

			// Code
			classBytecode[ctorIndex + 14] = 0x00;
			classBytecode[ctorIndex + 15] = 0x01;
			classBytecode[ctorIndex + 16] = 0x00;
			classBytecode[ctorIndex + 17] = 0x01;

			classBytecode[ctorIndex + 18] = 0x00;
			classBytecode[ctorIndex + 19] = 0x00;
			classBytecode[ctorIndex + 20] = 0x00;
			classBytecode[ctorIndex + 21] = 0x05;

			classBytecode[ctorIndex + 22] = ALOAD_0;
			classBytecode[ctorIndex + 23] = INVOKESPECIAL;
			classBytecode[ctorIndex + 24] = (byte)(baseClassCtorI >>> 8);
			classBytecode[ctorIndex + 25] = (byte)(baseClassCtorI);
			classBytecode[ctorIndex + 26] = RETURN;

			classBytecode[ctorIndex + 27] = 0x00;
			classBytecode[ctorIndex + 28] = 0x00;
			classBytecode[ctorIndex + 29] = 0x00;
			classBytecode[ctorIndex + 30] = 0x00;
		}

		{ // Add the custom method
			final int methodAccessFlags = Modifier.PUBLIC | Modifier.FINAL;

			classBytecode[methodIndex]     = (byte)(methodAccessFlags >>> 8);
			classBytecode[methodIndex + 1] = (byte)(methodAccessFlags);

			classBytecode[methodIndex + 2] = (byte)(methodNameI >>> 8);
			classBytecode[methodIndex + 3] = (byte)(methodNameI);
			classBytecode[methodIndex + 4] = (byte)(methodSignatureI >>> 8);
			classBytecode[methodIndex + 5] = (byte)(methodSignatureI);

			classBytecode[methodIndex + 6] = 0x00;
			classBytecode[methodIndex + 7] = 0x01;
			classBytecode[methodIndex + 8] = (byte)(codeAttributeI >>> 8);
			classBytecode[methodIndex + 9] = (byte)(codeAttributeI);

			final int length = end + 12;

			classBytecode[methodIndex + 10] = (byte)(length >>> 24);
			classBytecode[methodIndex + 11] = (byte)(length >>> 16);
			classBytecode[methodIndex + 12] = (byte)(length >>> 8);
			classBytecode[methodIndex + 13] = (byte)(length);

			// Code
			classBytecode[methodIndex + 14] = (byte)(stackSize >>> 8);
			classBytecode[methodIndex + 15] = (byte)(stackSize);
			classBytecode[methodIndex + 16] = (byte)(variableSize >>> 8);
			classBytecode[methodIndex + 17] = (byte)(variableSize);

			classBytecode[methodIndex + 18] = (byte)(end >>> 24);
			classBytecode[methodIndex + 19] = (byte)(end >>> 16);
			classBytecode[methodIndex + 20] = (byte)(end >>> 8);
			classBytecode[methodIndex + 21] = (byte)(end);

			System.arraycopy(bytecode, 0, classBytecode, methodIndex + 22, end);

			final int methodIndex2 = methodIndex + end;

			classBytecode[methodIndex2 + 22] = 0x00;
			classBytecode[methodIndex2 + 23] = 0x00;
			classBytecode[methodIndex2 + 24] = 0x00;
			classBytecode[methodIndex2 + 25] = 0x00;
		}

		// Attributes
		classBytecode[attributeIndex]     = 0x00;
		classBytecode[attributeIndex + 1] = 0x00;

		return (Class<T>)DEFINE_CLASS_METHOD.invoke(loader, name, classBytecode, 0, classBytecode.length);
	}

	/**
	 * Loads the specified method bytecode into the interface method and returns the new class.
	 *
	 * @param loader the loader to use for loading the new class
	 * @param bytecode the bytecode of the method
	 * @param stackSize the maximum size of the operand stack in the bytecode
	 * @param variableSize the maximum size of the local variables array in the bytecode
	 * @return the class with the bytecode loaded
	 * @throws IllegalAccessException if the loader cannot load the bytecode due to the current security setup
	 * @throws InvocationTargetException if the loader throws an exception while loading the bytecode
	 */
	public Class<T> load(final ClassLoader loader, final byte[] bytecode, final short stackSize, final short variableSize) throws IllegalAccessException, InvocationTargetException {
		return load(loader, bytecode, bytecode.length, stackSize, variableSize);
	}

}
