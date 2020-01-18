package horseshoe.internal;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

public final class MethodBuilder {

	private static final byte B0 = (byte)0;

	private static final byte STRING_CONSTANT = (byte)1;
	private static final byte INTEGER_CONSTANT = (byte)3;
	private static final byte FLOAT_CONSTANT = (byte)4;
	private static final byte LONG_CONSTANT = (byte)5;
	private static final byte DOUBLE_CONSTANT = (byte)6;
	private static final byte CLASS_CONSTANT = (byte)7;
	private static final byte STRING_REF_CONSTANT = (byte)8;
	private static final byte FIELD_CONSTANT = (byte)9;
	private static final byte METHOD_CONSTANT = (byte)10;
	private static final byte IMETHOD_CONSTANT = (byte)11;
	private static final byte NAME_AND_TYPE_CONSTANT = (byte)12;

	// Instructions that must be added through function calls
	private static final byte ANEWARRAY = (byte)0xBD; // 2: indexbyte1, indexbyte2 (stack: count -> arrayref)
	private static final byte CHECKCAST = (byte)0xC0; // 2: indexbyte1, indexbyte2 (stack: objectref -> objectref)
	private static final byte GETFIELD = (byte)0xB4; // 2: indexbyte1, indexbyte2 (stack: objectref -> value)
	private static final byte GETSTATIC = (byte)0xB2; // 2: indexbyte1, indexbyte2 (stack: -> value)
	private static final byte INSTANCEOF = (byte)0xC1; // 2: indexbyte1, indexbyte2 (stack: objectref -> result)
	private static final byte INVOKEINTERFACE = (byte)0xB9; // 4: indexbyte1, indexbyte2, count, 0 (stack: objectref, [arg1, arg2, ...] -> result)
	private static final byte INVOKESPECIAL = (byte)0xB7; // 2: indexbyte1, indexbyte2 (stack: objectref, [arg1, arg2, ...] -> result)
	private static final byte INVOKESTATIC = (byte)0xB8; // 2: indexbyte1, indexbyte2 (stack: [arg1, arg2, ...] -> result)
	private static final byte INVOKEVIRTUAL = (byte)0xB6; // 2: indexbyte1, indexbyte2 (stack: objectref, [arg1, arg2, ...] -> result)
	private static final byte LDC = (byte)0x12; // 1: index (stack: -> value)
	private static final byte LDC_W = (byte)0x13; // 2: indexbyte1, indexbyte2 (stack: -> value)
	private static final byte LDC2_W = (byte)0x14; // 2: indexbyte1, indexbyte2 (stack: -> value)
	private static final byte LOOKUPSWITCH = (byte)0xAB; // 8+: [0-3 bytes padding], defaultbyte1, defaultbyte2, defaultbyte3, defaultbyte4, npairs1, npairs2, npairs3, npairs4, match-offset pairs... (stack: key ->)
	private static final byte MULTIANEWARRAY = (byte)0xC5; // 3: indexbyte1, indexbyte2, dimensions (stack: count1, [count2,...] -> arrayref)
	private static final byte NEW = (byte)0xBB; // 2: indexbyte1, indexbyte2 (stack: -> objectref)
	private static final byte PUTFIELD = (byte)0xB5; // 2: indexbyte1, indexbyte2 (stack: objectref, value ->)
	private static final byte PUTSTATIC = (byte)0xB3; // 2: indexbyte1, indexbyte2 (stack: value ->)
	private static final byte TABLESWITCH = (byte)0xAA; // 16+: [0-3 bytes padding], defaultbyte1, defaultbyte2, defaultbyte3, defaultbyte4, lowbyte1, lowbyte2, lowbyte3, lowbyte4, highbyte1, highbyte2, highbyte3, highbyte4, jump offsets... (stack: index ->)

	// Do not use, added in Java 7, but we only support Java 6
	private static final byte INVOKEDYNAMIC = (byte)0xBA; // 4: indexbyte1, indexbyte2, 0, 0 (stack: [arg1, [arg2 ...]] -> result)

	// Instructions that can be added as code
	public static final byte AALOAD = (byte)0x32; // (stack: arrayref, index -> value)
	public static final byte AASTORE = (byte)0x53; // (stack: arrayref, index, value ->)
	public static final byte ACONST_NULL = (byte)0x01; // (stack: -> null)
	public static final byte ALOAD = (byte)0x19; // 1: index (stack: -> objectref)
	public static final byte ALOAD_0 = (byte)0x2A; // (stack: -> objectref)
	public static final byte ALOAD_1 = (byte)0x2B; // (stack: -> objectref)
	public static final byte ALOAD_2 = (byte)0x2C; // (stack: -> objectref)
	public static final byte ALOAD_3 = (byte)0x2D; // (stack: -> objectref)
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
	public static final byte LDIV = (byte)0x6D; // (stack: value1, value2 -> result)
	public static final byte LLOAD = (byte)0x16; // 1: index (stack: -> value)
	public static final byte LLOAD_0 = (byte)0x1E; // (stack: -> value)
	public static final byte LLOAD_1 = (byte)0x1F; // (stack: -> value)
	public static final byte LLOAD_2 = (byte)0x20; // (stack: -> value)
	public static final byte LLOAD_3 = (byte)0x21; // (stack: -> value)
	public static final byte LMUL = (byte)0x69; // (stack: value1, value2 -> result)
	public static final byte LNEG = (byte)0x75; // (stack: value -> result)
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
	public static final byte NEWARRAY = (byte)0xBC; // 1: atype (stack: count -> arrayref)
	public static final byte NOP = B0; // (stack: [No change])
	public static final byte POP = (byte)0x57; // (stack: value ->)
	public static final byte POP2 = (byte)0x58; // (stack: {value2, value1} ->)
	public static final byte RET = (byte)0xA9; // 1: index (stack: [No change])
	public static final byte RETURN = (byte)0xB1; // (stack: -> [empty])
	public static final byte SALOAD = (byte)0x35; // (stack: arrayref, index -> value)
	public static final byte SASTORE = (byte)0x56; // (stack: arrayref, index, value ->)
	public static final byte SIPUSH = (byte)0x11; // 2: byte1, byte2 (stack: -> value)
	public static final byte SWAP = (byte)0x5F; // (stack: value2, value1 -> value1, value2)
	public static final byte WIDE = (byte)0xC4; // 3/5: opcode, indexbyte1, indexbyte2 (stack: [same as for corresponding instructions])

	private byte[] bytes = new byte[256];
	private int length = 0;
	private int maxStackSize = 0;
	private int stackSize = 0;
	private int maxLocalVariableIndex = 0; // Always include index 0 to support "this" pointer to support non-static methods
	private final Set<Label> labels = new LinkedHashSet<>();

	private static final class Location {
		public Object container;
		public int offset;
		public int updateOffset;

		/**
		 * Creates a new location using a builder as the container.
		 *
		 * @param container the builder that contains the location
		 * @param offset the offset within the byte buffer of the container used in offset calculations
		 * @param updateOffset the offset within the byte buffer of the container updated with the appropriate information
		 */
		public Location(final MethodBuilder container, final int offset, final int updateOffset) {
			this.container = container;
			this.offset = offset;
			this.updateOffset = updateOffset;
		}

		/**
		 * Creates a new location using a builder as the container.
		 *
		 * @param container the builder that contains the location
		 * @param offset the offset within the byte buffer of the container
		 */
		public Location(final MethodBuilder container, final int offset) {
			this.container = container;
			this.offset = offset;
			this.updateOffset = offset;
		}

		/**
		 * Creates a new location using a constant pool as the container.
		 *
		 * @param container the constant pool that contains the location
		 * @param offset the offset within the byte buffer of the container
		 */
		public Location(final ConstantPool container, final int offset) {
			this.container = container;
			this.offset = offset;
			this.updateOffset = offset;
		}

		/**
		 * Updates the location using the specified values.
		 *
		 * @param container the builder that contains the location
		 * @param additionalOffset the additional offset within the byte buffer of the container used in offset calculations
		 * @return this location
		 */
		public Location update(final MethodBuilder container, final int additionalOffset) {
			this.container = container;
			this.offset += additionalOffset;
			this.updateOffset += additionalOffset;
			return this;
		}
	}

	private static final class ConstantPoolEntry {
		public final short index;
		public final List<Location> locations = new ArrayList<>(64);

		/**
		 * Creates a new constant pool entry.
		 *
		 * @param index the index of the entry within the constant pool
		 */
		public ConstantPoolEntry(final short index) {
			this.index = index;
		}

		/**
		 * Adds a new update location to the constant pool entry.
		 *
		 * @param location the update location to add to the constant pool entry
		 * @return this entry
		 */
		public ConstantPoolEntry add(final Location location) {
			locations.add(location);
			return this;
		}
	}

	private static abstract class ConstantPool {
		/**
		 * Adds data to the constant pool. The constant pool length is automatically extended.
		 *
		 * @param object the object to add to the constant pool
		 * @param data the data to append to the constant pool
		 * @return the newly created data
		 */
		public abstract ConstantPoolEntry add(final Object object, final byte... data);

		/**
		 * Clears the constant pool.
		 *
		 * @return this constant pool
		 */
		public abstract ConstantPool clear();

		/**
		 * Gets the constant pool count.
		 *
		 * @return the constant pool count
		 */
		public abstract int count();

		/**
		 * Gets data from the constant pool.
		 *
		 * @param object the object to lookup in the constant pool
		 * @return the data in the constant pool, or null if it does not exist
		 */
		public abstract ConstantPoolEntry get(final Object object);

		/**
		 * Gets the byte data for the constant pool.
		 *
		 * @return the byte data for the constant pool
		 */
		public abstract byte[] getData();

		/**
		 * Gets the set of all entries in the constant pool.
		 *
		 * @return the set of all entries in the constant pool
		 */
		public abstract Set<Entry<Object, ConstantPoolEntry>> getEntries();

		/**
		 * Gets the length of the constant pool.
		 *
		 * @return the length of the constant pool
		 */
		public abstract int getLength();

		/**
		 * Populates the data dependent on the set of constants in the constant pool.
		 *
		 * @return this constant pool
		 */
		public abstract ConstantPool populate();
	}

	public static abstract class Label {
		/**
		 * Adds a reference to the label.
		 *
		 * @param builder the builder that references the label
		 * @param offset the offset of the reference within the builder
		 * @param updateOffset the offset within the builder that gets updated with the branch offset
		 * @return this label
		 */
		public abstract Label addReference(final MethodBuilder builder, final int offset, final int updateOffset);

		/**
		 * Gets an iterator to all references to the label.
		 *
		 * @return an iterator to all references to the label
		 */
		public abstract Iterable<Location> getReferences();

		/**
		 * Gets the target for the label.
		 *
		 * @return the target for the label
		 */
		public abstract Location getTarget();

		/**
		 * Populates the data dependent on this label.
		 *
		 * @return this label
		 */
		public abstract Label populate();

		/**
		 * Sets a new target for the label.
		 *
		 * @param target the new target for the label
		 * @return this label
		 */
		public abstract Label setTarget(final Location target);
	}

	private static final class UTF8String {
		private final String value;

		public UTF8String(final String value) {
			this.value = value;
		}

		@Override
		public boolean equals(final Object obj) {
			return obj instanceof UTF8String && value.equals(((UTF8String)obj).value);
		}

		@Override
		public int hashCode() {
			return UTF8String.class.hashCode() ^ value.hashCode();
		}

		@Override
		public String toString() {
			return value;
		}
	}

	private static final class BytecodeLoader<U> extends ClassLoader {
		public BytecodeLoader(final ClassLoader parent) {
			super(parent);
		}

		@SuppressWarnings("unchecked")
		public Class<U> defineClass(final String name, final byte[] bytecode) {
			return (Class<U>)super.defineClass(name, bytecode, 0, bytecode.length);
		}
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

	/**
	 * Gets the stack offset that results from invoking (or getting) a member (field, method, constructor). The object for non-static members is not factored into the stack offset.
	 *
	 * @param member the member to analyze
	 * @return the stack offset that results from invoking (or getting) the member
	 */
	private static int getCallStackOffset(final Member member) {
		if (member instanceof Field) {
			final Field field = (Field)member;
			return double.class.equals(field.getType()) || long.class.equals(field.getType()) ? 2 : 1;
		}

		if (member instanceof Method) {
			final Method method = (Method)member;
			int offset = double.class.equals(method.getReturnType()) || long.class.equals(method.getReturnType()) ? 2 : 1;

			for (final Class<?> type : method.getParameterTypes()) {
				offset -= double.class.equals(type) || long.class.equals(type) ? 2 : void.class.equals(type) ? 0 : 1;
			}

			return offset;
		}

		final Constructor<?> constructor = (Constructor<?>)member;
		int offset = 0;

		for (final Class<?> type : constructor.getParameterTypes()) {
			offset -= double.class.equals(type) || long.class.equals(type) ? 2 : 1;
		}

		return offset;
	}

	private final ConstantPool constantPool = new ConstantPool() {
		private final Map<Object, ConstantPoolEntry> constants = new LinkedHashMap<>();
		private byte[] buffer = new byte[256];
		private int length = 0;

		@Override
		public ConstantPoolEntry add(final Object object, final byte... data) {
			final ConstantPoolEntry info = new ConstantPoolEntry((short)(constants.size() + 1));
			constants.put(object, info);

			if (length + data.length > buffer.length) {
				final byte[] newBuffer = new byte[Math.max(length + data.length, buffer.length * 2)];
				System.arraycopy(buffer, 0, newBuffer, 0, length);
				buffer = newBuffer;
			}

			System.arraycopy(data, 0, buffer, length, data.length);
			length += data.length;

			return info;
		}

		@Override
		public ConstantPool clear() {
			constants.clear();
			length = 0;
			return this;
		}

		@Override
		public int count() {
			return constants.size() + 1;
		}

		@Override
		public ConstantPoolEntry get(final Object object) {
			return constants.get(object);
		}

		@Override
		public byte[] getData() {
			return buffer;
		}

		@Override
		public Set<Entry<Object, ConstantPoolEntry>> getEntries() {
			return constants.entrySet();
		}

		@Override
		public int getLength() {
			return length;
		}

		@Override
		public ConstantPool populate() {
			for (final ConstantPoolEntry entry : constants.values()) {
				for (final Location location : entry.locations) {
					final byte bytes[] = (location.container == this ? buffer : ((MethodBuilder)location.container).bytes);

					bytes[location.offset]     = (byte)(entry.index >>> 8);
					bytes[location.offset + 1] = (byte)(entry.index);
				}
			}

			return this;
		}
	};

	/**
	 * Adds an access (load / store) instruction. This is a convenience method that applies the "wide" instruction prefix if needed.
	 *
	 * @param instruction the load or store instruction
	 * @param index the index of the local variable to access
	 * @return this builder
	 */
	public MethodBuilder addAccess(final byte instruction, final int index) {
		return index < Byte.MIN_VALUE || index > Byte.MAX_VALUE ? addCode(WIDE, instruction, (byte)(index >>> 8), (byte)index) : addCode(instruction, (byte)index);
	}

	/**
	 * Adds a branch instruction to the given label. Note that the label must be in the same buffer or it must be combined with the buffer containing the label at some point.
	 *
	 * @param instruction the branch instruction
	 * @param label the label to branch to
	 * @return this builder
	 */
	public MethodBuilder addBranch(final byte instruction, final Label label) {
		labels.add(label);

		switch (instruction) {
		case GOTO:
		case GOTO_W:
			label.addReference(this, length, length + 1);
			return append(GOTO, B0, B0);

		case IF_ACMPEQ:
		case IF_ACMPNE:
		case IF_ICMPEQ:
		case IF_ICMPGE:
		case IF_ICMPGT:
		case IF_ICMPLE:
		case IF_ICMPLT:
		case IF_ICMPNE:
			label.addReference(this, length, length + 1);
			stackSize -= 2;
			return append(instruction, B0, B0);

		case IFEQ:
		case IFGE:
		case IFGT:
		case IFLE:
		case IFLT:
		case IFNE:
		case IFNONNULL:
		case IFNULL:
			label.addReference(this, length, length + 1);
			stackSize--;
			return append(instruction, B0, B0);

		case JSR:
		case JSR_W:
			label.addReference(this, length, length + 1);
			maxStackSize = Math.max(maxStackSize, ++stackSize);
			return addCode(JSR, B0, B0);

		default:
			throw new RuntimeException("Unexpected bytecode instruction: 0x" + Integer.toHexString(instruction) + ", expecting a branch instruction");
		}
	}

	/**
	 * Adds a cast to the specified type.
	 *
	 * @param type the type to cast to
	 * @return this builder
	 */
	public MethodBuilder addCast(final Class<?> type) {
		getConstant(type).add(new Location(this, length + 1));
		return append(CHECKCAST, B0, B0);
	}

	/**
	 * Adds code to the builder, reserving extra space in the builder if necessary. The length is automatically extended.
	 *
	 * @param code the code to append to the builder
	 * @return this builder
	 */
	public MethodBuilder addCode(final byte... code) {
		int stackOffset = 0;
		int i = 0;

		for (; i < code.length; i++) {
			switch (code[i]) {
			case DALOAD:
			case LALOAD: break;
			case AALOAD:
			case BALOAD:
			case CALOAD:
			case FALOAD:
			case IALOAD:
			case SALOAD: stackOffset--; break;

			case DASTORE:
			case LASTORE: stackOffset--;
			case AASTORE:
			case BASTORE:
			case CASTORE:
			case FASTORE:
			case IASTORE:
			case SASTORE: stackOffset -= 3; break;

			case DCONST_0:
			case DCONST_1:
			case LCONST_0:
			case LCONST_1: stackOffset++;
			case ACONST_NULL:
			case FCONST_0:
			case FCONST_1:
			case FCONST_2:
			case ICONST_M1:
			case ICONST_0:
			case ICONST_1:
			case ICONST_2:
			case ICONST_3:
			case ICONST_4:
			case ICONST_5: stackOffset++; break;

			case DLOAD:
			case LLOAD: stackOffset++;
			case ALOAD:
			case FLOAD:
			case ILOAD: stackOffset++; maxLocalVariableIndex = Math.max(maxLocalVariableIndex, code[++i] & 0xFF); break;

			case DLOAD_0:
			case LLOAD_0: stackOffset++;
			case ALOAD_0:
			case FLOAD_0:
			case ILOAD_0: stackOffset++; break;
			case DLOAD_1:
			case LLOAD_1: stackOffset++;
			case ALOAD_1:
			case FLOAD_1:
			case ILOAD_1: stackOffset++; maxLocalVariableIndex = Math.max(maxLocalVariableIndex, 1); break;
			case DLOAD_2:
			case LLOAD_2: stackOffset++;
			case ALOAD_2:
			case FLOAD_2:
			case ILOAD_2: stackOffset++; maxLocalVariableIndex = Math.max(maxLocalVariableIndex, 2); break;
			case DLOAD_3:
			case LLOAD_3: stackOffset++;
			case ALOAD_3:
			case FLOAD_3:
			case ILOAD_3: stackOffset++; maxLocalVariableIndex = Math.max(maxLocalVariableIndex, 3); break;

			case ARETURN:
			case DRETURN:
			case FRETURN:
			case IRETURN:
			case LRETURN:
			case RETURN: break;

			case DSTORE:
			case LSTORE: stackOffset--;
			case ASTORE:
			case FSTORE:
			case ISTORE: stackOffset--; maxLocalVariableIndex = Math.max(maxLocalVariableIndex, code[++i] & 0xFF); break;

			case DSTORE_0:
			case LSTORE_0: stackOffset--;
			case ASTORE_0:
			case FSTORE_0:
			case ISTORE_0: stackOffset--; break;
			case DSTORE_1:
			case LSTORE_1: stackOffset--;
			case ASTORE_1:
			case FSTORE_1:
			case ISTORE_1: stackOffset--; maxLocalVariableIndex = Math.max(maxLocalVariableIndex, 1); break;
			case DSTORE_2:
			case LSTORE_2: stackOffset--;
			case ASTORE_2:
			case FSTORE_2:
			case ISTORE_2: stackOffset--; maxLocalVariableIndex = Math.max(maxLocalVariableIndex, 2); break;
			case DSTORE_3:
			case LSTORE_3: stackOffset--;
			case ASTORE_3:
			case FSTORE_3:
			case ISTORE_3: stackOffset--; maxLocalVariableIndex = Math.max(maxLocalVariableIndex, 3); break;

			case D2F:
			case D2I:
			case L2F:
			case L2I: stackOffset--; break;
			case D2L:
			case F2I:
			case I2B:
			case I2C:
			case I2F:
			case I2S:
			case L2D: break;
			case F2D:
			case F2L:
			case I2D:
			case I2L: stackOffset++; break;

			case DADD:
			case DDIV:
			case DMUL:
			case DREM:
			case DSUB:
			case LADD:
			case LAND:
			case LDIV:
			case LMUL:
			case LOR:
			case LREM:
			case LSUB:
			case LXOR: stackOffset--;
			case FADD:
			case FDIV:
			case FMUL:
			case FREM:
			case FSUB:
			case IADD:
			case IAND:
			case IDIV:
			case IMUL:
			case IOR:
			case IREM:
			case ISHL:
			case ISHR:
			case ISUB:
			case IUSHR:
			case IXOR:
			case LSHL:
			case LSHR:
			case LUSHR: stackOffset--; break;

			case DCMPG:
			case DCMPL:
			case LCMP: stackOffset -= 2;
			case FCMPG:
			case FCMPL: stackOffset--; break;

			case DNEG:
			case FNEG:
			case INEG:
			case LNEG: break;

			case DUP:
			case DUP_X1:
			case DUP_X2: stackOffset++; break;
			case DUP2:
			case DUP2_X1:
			case DUP2_X2: stackOffset += 2; break;
			case POP: stackOffset--; break;
			case POP2: stackOffset -= 2; break;

			case GETFIELD:
			case GETSTATIC:
			case PUTFIELD:
			case PUTSTATIC: throw new RuntimeException("All field accesses must use the addFieldAccess() method");

			case GOTO:
			case GOTO_W:
			case IF_ACMPEQ:
			case IF_ACMPNE:
			case IF_ICMPEQ:
			case IF_ICMPGE:
			case IF_ICMPGT:
			case IF_ICMPLE:
			case IF_ICMPLT:
			case IF_ICMPNE:
			case IFEQ:
			case IFGE:
			case IFGT:
			case IFLE:
			case IFLT:
			case IFNE:
			case IFNONNULL:
			case IFNULL:
			case JSR:
			case JSR_W: throw new RuntimeException("All branch instructions must use the addBranch() method");

			case LOOKUPSWITCH:
			case TABLESWITCH: throw new RuntimeException("All switch instructions must use the addSwitch() method");

			case INVOKEDYNAMIC:
			case INVOKEINTERFACE:
			case INVOKESPECIAL:
			case INVOKESTATIC:
			case INVOKEVIRTUAL: throw new RuntimeException("All invocations must use the addInvoke() method");

			case CHECKCAST: throw new RuntimeException("All casts must use the addCast() method");
			case INSTANCEOF: throw new RuntimeException("All instanceof checks must use the addInstanceOfCheck() method");

			case LDC:
			case LDC_W:
			case LDC2_W: throw new RuntimeException("All constant loads must use the pushConstant() method");

			case NEWARRAY: i++; break;
			case ANEWARRAY:
			case MULTIANEWARRAY:
			case NEW: throw new RuntimeException("All new's must use the pushNewObject() method");

			case ARRAYLENGTH: break;
			case ATHROW: break;
			case BIPUSH: stackOffset++; i++; break;
			case BREAKPOINT: break;
			case IINC: stackOffset -= 2; maxLocalVariableIndex = Math.max(maxLocalVariableIndex, code[++i] & 0xFF); i++; break;
			case MONITORENTER: stackOffset--; break;
			case MONITOREXIT: stackOffset--; break;
			case NOP: break;
			case RET: maxLocalVariableIndex = Math.max(maxLocalVariableIndex, code[++i] & 0xFF); i++; break;
			case SIPUSH: stackOffset++; i += 2; break;
			case SWAP: break;

			case WIDE:
				switch (code[++i]) {
				case DLOAD:
				case LLOAD: stackOffset++;
				case ALOAD:
				case FLOAD:
				case ILOAD: stackOffset++; maxLocalVariableIndex = Math.max(maxLocalVariableIndex, ((code[i + 1] & 0xFF) << 8) + (code[i + 2] & 0xFF)); i += 2; break;

				case DSTORE:
				case LSTORE: stackOffset--;
				case ASTORE:
				case FSTORE:
				case ISTORE: stackOffset--; maxLocalVariableIndex = Math.max(maxLocalVariableIndex, ((code[i + 1] & 0xFF) << 8) + (code[i + 2] & 0xFF)); i += 2; break;

				case IINC: maxLocalVariableIndex = Math.max(maxLocalVariableIndex, ((code[i + 1] & 0xFF) << 8) + (code[i + 2] & 0xFF)); i += 4; break;
				case RET:  maxLocalVariableIndex = Math.max(maxLocalVariableIndex, ((code[i + 1] & 0xFF) << 8) + (code[i + 2] & 0xFF)); i += 2; break;

				default: throw new RuntimeException("Unrecognized wide bytecode instruction: 0x" + Integer.toHexString(code[i]));
			}

			default: throw new RuntimeException("Unrecognized bytecode instruction: 0x" + Integer.toHexString(code[i]));
			}

			maxStackSize = Math.max(maxStackSize, stackSize + stackOffset);
		}

		if (i != code.length) {
			throw new RuntimeException("Invalid bytecode length");
		}

		this.stackSize += stackOffset;
		return append(code);
	}

	/**
	 * Adds code to access (load or store) the specified field.
	 *
	 * @param field the field to access
	 * @param load true to load the value of the field, false to store the value into the field
	 * @return this builder
	 */
	public MethodBuilder addFieldAccess(final Field field, final boolean load) {
		getConstant(field).add(new Location(this, length + 1));

		if (Modifier.isStatic(field.getModifiers())) {
			if (load) {
				stackSize += double.class.equals(field.getType()) || long.class.equals(field.getType()) ? 2 : 1;
				maxStackSize = Math.max(maxStackSize, stackSize);
				return append(GETSTATIC, B0, B0);
			} else {
				stackSize -= double.class.equals(field.getType()) || long.class.equals(field.getType()) ? 2 : 1;
				return append(PUTSTATIC, B0, B0);
			}
		} else if (load) {
			stackSize += double.class.equals(field.getType()) || long.class.equals(field.getType()) ? 1 : 0;
			maxStackSize = Math.max(maxStackSize, stackSize);
			return append(GETFIELD, B0, B0);
		}

		stackSize -= double.class.equals(field.getType()) || long.class.equals(field.getType()) ? 3 : 2;
		return append(PUTFIELD, B0, B0);
	}

	/**
	 * Adds an instanceof check.
	 *
	 * @param type the type to check
	 * @return this builder
	 */
	public MethodBuilder addInstanceOfCheck(final Class<?> type) {
		getConstant(type).add(new Location(this, length + 1));
		return append(INSTANCEOF, B0, B0);
	}

	/**
	 * Adds invocation code for the specified method.
	 *
	 * @param method the method to invoke
	 * @param superCall true to invoke the method on the super class, otherwise false
	 * @return this builder
	 */
	public MethodBuilder addInvoke(final Method method, final boolean superCall) {
		getConstant(method).add(new Location(this, length + 1));
		final int modifiers = method.getModifiers();

		if (Modifier.isStatic(modifiers)) {
			stackSize += getCallStackOffset(method);
			maxStackSize = Math.max(maxStackSize, stackSize);
			return append(INVOKESTATIC, B0, B0);
		} else if (Modifier.isInterface(method.getDeclaringClass().getModifiers())) {
			final int stackOffset = getCallStackOffset(method);
			stackSize += stackOffset - 1;
			maxStackSize = Math.max(maxStackSize, stackSize);
			return append(INVOKEINTERFACE, B0, B0, (byte)((double.class.equals(method.getReturnType()) || long.class.equals(method.getReturnType()) ? 3 : void.class.equals(method.getReturnType()) ? 1 : 2) - stackOffset), B0);
		} else if (Modifier.isPrivate(modifiers) || superCall) {
			stackSize += getCallStackOffset(method) - 1;
			maxStackSize = Math.max(maxStackSize, stackSize);
			return append(INVOKESPECIAL, B0, B0);
		}

		stackSize += getCallStackOffset(method) - 1;
		maxStackSize = Math.max(maxStackSize, stackSize);
		return append(INVOKEVIRTUAL, B0, B0);
	}

	/**
	 * Adds invocation code for the specified method.
	 *
	 * @param method the method to invoke
	 * @return this builder
	 */
	public MethodBuilder addInvoke(final Method method) {
		return addInvoke(method, false);
	}

	/**
	 * Adds invocation code for the specified constructor.
	 *
	 * @param constructor the constructor to invoke
	 * @return this builder
	 */
	public MethodBuilder addInvoke(final Constructor<?> constructor) {
		getConstant(constructor).add(new Location(this, length + 1));

		stackSize += getCallStackOffset(constructor) - 1;
		maxStackSize = Math.max(maxStackSize, stackSize);
		return append(INVOKESPECIAL, B0, B0);
	}

	/**
	 * Adds a primitive conversion. This handles narrowing and widening conversions as well as boxing / unboxing conversions.
	 *
	 * @param from the class to convert from
	 * @param to the class to convert to
	 * @return this builder
	 */
	public MethodBuilder addPrimitiveConversion(final Class<?> from, final Class<?> to) {
		try {
			if (!to.isPrimitive()) {
				if (int.class.equals(from) && to.isAssignableFrom(Integer.class)) {
					return addInvoke(Integer.class.getMethod("valueOf", int.class));
				} else if (short.class.equals(from) && to.isAssignableFrom(Short.class)) {
					return addInvoke(Short.class.getMethod("valueOf", short.class));
				} else if (byte.class.equals(from) && to.isAssignableFrom(Byte.class)) {
					return addInvoke(Byte.class.getMethod("valueOf", byte.class));
				} else if (boolean.class.equals(from) && to.isAssignableFrom(Boolean.class)) {
					return addInvoke(Boolean.class.getMethod("valueOf", boolean.class));
				} else if (char.class.equals(from) && to.isAssignableFrom(Character.class)) {
					return addInvoke(Character.class.getMethod("valueOf", char.class));
				} else if (long.class.equals(from) && to.isAssignableFrom(Long.class)) {
					return addInvoke(Long.class.getMethod("valueOf", long.class));
				} else if (float.class.equals(from) && to.isAssignableFrom(Float.class)) {
					return addInvoke(Float.class.getMethod("valueOf", float.class));
				} else if (double.class.equals(from) && to.isAssignableFrom(Double.class)) {
					return addInvoke(Double.class.getMethod("valueOf", double.class));
				}
			} else if (boolean.class.equals(to)) {
				if (int.class.equals(from) || short.class.equals(from) || byte.class.equals(from) || boolean.class.equals(from) || char.class.equals(from)) {
					return this;
				} else if (long.class.equals(from)) {
					return addCode(LCONST_0, LCMP);
				} else if (float.class.equals(from)) {
					return addCode(FCONST_0, FCMPG);
				} else if (double.class.equals(from)) {
					return addCode(DCONST_0, DCMPG);
				} else if (Boolean.class.equals(from)) {
					return addInvoke(Boolean.class.getMethod("booleanValue"));
				}
			} else if (Number.class.isAssignableFrom(from)) {
				if (int.class.equals(to)) {
					return addInvoke(Integer.class.getMethod("intValue"));
				} else if (short.class.equals(to)) {
					return addInvoke(Short.class.getMethod("shortValue"));
				} else if (byte.class.equals(to)) {
					return addInvoke(Byte.class.getMethod("byteValue"));
				} else if (long.class.equals(to)) {
					return addInvoke(Long.class.getMethod("longValue"));
				} else if (float.class.equals(to)) {
					return addInvoke(Float.class.getMethod("floatValue"));
				} else if (double.class.equals(to)) {
					return addInvoke(Double.class.getMethod("doubleValue"));
				}
			} else if (Character.class.equals(from)) {
				if (char.class.equals(to)) {
					return addInvoke(Character.class.getMethod("charValue"));
				}
			} else if (int.class.equals(from) || short.class.equals(from) || byte.class.equals(from) || boolean.class.equals(from) || char.class.equals(from)) {
				if (int.class.equals(to)) {
					return this;
				} else if (short.class.equals(to)) {
					return addCode(I2S);
				} else if (byte.class.equals(to) || boolean.class.equals(to)) {
					return addCode(I2B);
				} else if (char.class.equals(to)) {
					return addCode(I2C);
				} else if (long.class.equals(to)) {
					return addCode(I2L);
				} else if (float.class.equals(to)) {
					return addCode(I2F);
				} else if (double.class.equals(to)) {
					return addCode(I2D);
				}
			} else if (long.class.equals(from)) {
				if (int.class.equals(to) || short.class.equals(to) || byte.class.equals(to) || boolean.class.equals(to) || char.class.equals(to)) {
					return addCode(L2I).addPrimitiveConversion(int.class, to);
				} else if (long.class.equals(to)) {
					return this;
				} else if (float.class.equals(to)) {
					return addCode(L2F);
				} else if (double.class.equals(to)) {
					return addCode(L2D);
				}
			} else if (float.class.equals(from)) {
				if (int.class.equals(to) || short.class.equals(to) || byte.class.equals(to) || boolean.class.equals(to) || char.class.equals(to)) {
					return addCode(F2I).addPrimitiveConversion(int.class, to);
				} else if (long.class.equals(to)) {
					return addCode(F2L);
				} else if (float.class.equals(to)) {
					return this;
				} else if (double.class.equals(to)) {
					return addCode(F2D);
				}
			} else if (double.class.equals(from)) {
				if (int.class.equals(to) || short.class.equals(to) || byte.class.equals(to) || boolean.class.equals(to) || char.class.equals(to)) {
					return addCode(D2I).addPrimitiveConversion(int.class, to);
				} else if (long.class.equals(to)) {
					return addCode(D2L);
				} else if (float.class.equals(to)) {
					return addCode(D2F);
				} else if (double.class.equals(to)) {
					return this;
				}
			}
		} catch (final ReflectiveOperationException e) {
			throw new RuntimeException(e.getMessage(), e); // This should never happen
		}

		throw new IllegalArgumentException();
	}

	/**
	 * Adds a switch instruction. Note that all labels must be in the same buffer or must be combined with the buffer containing the label at some point.
	 *
	 * @param labels the map of labels to branch to when the stack value matches the maps key
	 * @param defaultLabel the default label to branch to when the stack value does not match any of the keys in labels
	 * @return this builder
	 */
	public MethodBuilder addSwitch(final SortedMap<Integer, Label> labels, final Label defaultLabel) {
		while (length % 4 != 3) {
			append(NOP);
		}

		final int count = labels.size();
		final int start = length;

		defaultLabel.addReference(this, start, length + 3);
		append(LOOKUPSWITCH, B0, B0, B0, B0, (byte)(count >>> 24), (byte)(count >>> 16), (byte)(count >>> 8), (byte)count);

		for (final Entry<Integer, Label> entry : labels.entrySet()) {
			final int key = entry.getKey();

			entry.getValue().addReference(this, start, length + 6);
			append((byte)(key >>> 24), (byte)(key >>> 16), (byte)(key >>> 8), (byte)key, B0, B0, B0, B0);
		}

		return this;
	}

	/**
	 * Adds a throw instruction. The throwable is constructed using the specified parameters.
	 *
	 * @param throwable the class of the throwable to throw
	 * @param message the message to use for constructing the throwable
	 * @return this builder
	 */
	public MethodBuilder addThrow(final Class<? extends Throwable> throwable, final String message) {
		try {
			if (message == null) {
				return pushNewObject(throwable).addCode(DUP).addInvoke(throwable.getConstructor()).addCode(ATHROW);
			} else {
				return pushNewObject(throwable).addCode(DUP).pushConstant(message).addInvoke(throwable.getConstructor(String.class)).addCode(ATHROW);
			}
		} catch (final ReflectiveOperationException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * Appends another builder to this builder, reserving extra space in the buffer if necessary. The length is extended and the other buffer is cleared.
	 *
	 * @param other the buffer to append
	 * @return this builder
	 */
	public MethodBuilder append(final MethodBuilder other) {
		if (other == this) {
			throw new IllegalArgumentException("Cannot append a builder to itself");
		}

		maxStackSize = Math.max(maxStackSize, stackSize + other.maxStackSize);
		stackSize += other.stackSize;
		maxLocalVariableIndex = Math.max(maxLocalVariableIndex, other.maxLocalVariableIndex);

		// Align the buffer (to support switch instructions), then append the other builder's buffer
		while (length % 4 != 0) {
			append(NOP);
		}

		final int oldLength = length;
		append(other.bytes, other.length);

		// Update all labels to point to this buffer
		for (final Label label : labels) {
			final Location target = label.getTarget();

			// Update the target if needed
			if (target.container == other) {
				target.update(this, oldLength);
			}

			// Update each location if needed
			for (final Location location : label.getReferences()) {
				if (location.container == other) {
					location.update(this, oldLength);
				}
			}
		}

		// Update all labels for the other builder to point to this buffer
		for (final Label label : other.labels) {
			final Location target = label.getTarget();

			// Update the target if needed
			if (target.container == other) {
				target.update(this, oldLength);
			}

			// Update each location if needed
			for (final Location location : label.getReferences()) {
				if (location.container == other) {
					location.update(this, oldLength);
				}
			}

			labels.add(label);
		}

		// Pull in constants from the other builder
		for (final Entry<Object, ConstantPoolEntry> entry : other.constantPool.getEntries()) {
			List<Location> newLocations = null;

			// Only pull in constants that modify the buffer
			for (final Location location : entry.getValue().locations) {
				if (location.container == other) {
					if (newLocations == null) {
						newLocations = getConstant(entry.getKey()).locations;
					}

					newLocations.add(location.update(this, oldLength));
				}
			}
		}

		// Reset the other builder to avoid issues with updating labels and constant entries
		other.reset();
		return this;
	}

	/**
	 * Adds the data to the builder, reserving extra space in the buffer if necessary. The length is automatically extended.
	 *
	 * @param data the data to append to the builder
	 * @param length the length to append to the builder
	 * @return this builder
	 */
	private MethodBuilder append(final byte[] data, final int length) {
		if (this.length + length > bytes.length) {
			final byte[] newBuffer = new byte[Math.max(this.length + length, bytes.length * 2)];
			System.arraycopy(bytes, 0, newBuffer, 0, this.length);
			bytes = newBuffer;
		}

		System.arraycopy(data, 0, bytes, this.length, length);
		this.length += length;
		return this;
	}

	/**
	 * Adds the data to the buffer, reserving extra space in the buffer if necessary. The length is automatically extended.
	 *
	 * @param data the data to append to the buffer
	 * @return this builder
	 */
	private MethodBuilder append(final byte... data) {
		return append(data, data.length);
	}

	/**
	 * Gets the specified object from the constant pool. If the object does not already exist in the constant pool, it will be added and the newly created information returned.
	 *
	 * @param value the object to get from the constant pool
	 * @return the constant pool information for the object
	 */
	private ConstantPoolEntry getConstant(final Object value) {
		final ConstantPoolEntry info = constantPool.get(value);

		if (info != null) {
			return info;
		}

		if (value instanceof Integer) {
			final int number = (Integer)value;
			return constantPool.add(value, INTEGER_CONSTANT,
					(byte)(number >>> 24), (byte)(number >>> 16), (byte)(number >>> 8),  (byte)number);
		} else if (value instanceof Long) {
			final long number = (Long)value;
			final ConstantPoolEntry entry = constantPool.add(value, LONG_CONSTANT,
					(byte)(number >>> 56), (byte)(number >>> 48), (byte)(number >>> 40), (byte)(number >>> 32),
					(byte)(number >>> 24), (byte)(number >>> 16), (byte)(number >>> 8),  (byte)number);
			constantPool.add(new Object());
			return entry;
		} else if (value instanceof Float) {
			final long number = Float.floatToRawIntBits((Float)value);
			return constantPool.add(value, FLOAT_CONSTANT,
					(byte)(number >>> 24), (byte)(number >>> 16), (byte)(number >>> 8),  (byte)number);
		} else if (value instanceof Double) {
			final long number = Double.doubleToRawLongBits((Double)value);
			final ConstantPoolEntry entry = constantPool.add(value, DOUBLE_CONSTANT,
					(byte)(number >>> 56), (byte)(number >>> 48), (byte)(number >>> 40), (byte)(number >>> 32),
					(byte)(number >>> 24), (byte)(number >>> 16), (byte)(number >>> 8),  (byte)number);
			constantPool.add(new Object());
			return entry;
		} else if (value instanceof UTF8String) {
			final byte[] utfChars = value.toString().getBytes(StandardCharsets.UTF_8); // Note no length or invalid character checks
			final byte[] data = Arrays.copyOf(new byte[] { STRING_CONSTANT, (byte)(utfChars.length >>> 8), (byte)(utfChars.length) }, 3 + utfChars.length);
			System.arraycopy(utfChars, 0, data, 3, utfChars.length);
			return constantPool.add(value, data);
		} else if (value instanceof String) {
			getConstant(new UTF8String(value.toString())).add(new Location(constantPool, constantPool.getLength() + 1));
			return constantPool.add(value, STRING_REF_CONSTANT, B0, B0);
		} else if (value instanceof Class) {
			getConstant(new UTF8String(((Class<?>)value).getName().replace('.', '/'))).add(new Location(constantPool, constantPool.getLength() + 1));
			return constantPool.add(value, CLASS_CONSTANT, B0, B0);
		} else if (value instanceof Member) {
			final Class<?> parentClass = (value instanceof Method ? ((Method)value).getDeclaringClass() :
				value instanceof Constructor ? ((Constructor<?>)value).getDeclaringClass() :
				value instanceof Field ? ((Field)value).getDeclaringClass() : null);

			// Build the signature
			final Member member = (Member)value;
			final ConstantPoolEntry name = getConstant(new UTF8String(value instanceof Constructor ? "<init>" : member.getName()));
			final ConstantPoolEntry signature = getConstant(new UTF8String(getSignature(member)));
			final ConstantPoolEntry declaringClass = getConstant(parentClass);

			name.add(new Location(constantPool, constantPool.getLength() + 1));
			signature.add(new Location(constantPool, constantPool.getLength() + 3));
			constantPool.add(new Object(), NAME_AND_TYPE_CONSTANT, B0, B0, B0, B0).add(new Location(constantPool, constantPool.getLength() + 3));

			declaringClass.add(new Location(constantPool, constantPool.getLength() + 1));
			return constantPool.add(value, (value instanceof Field ? FIELD_CONSTANT : (parentClass.getModifiers() & Modifier.INTERFACE) == 0 ? METHOD_CONSTANT : IMETHOD_CONSTANT), B0, B0, B0, B0);
		}

		throw new RuntimeException("Cannot add a constant of type " + value.getClass().toString());
	}

	/**
	 * Loads the bytecode of the method builder into a new class.
	 * @param <T> the type of the base class
	 * @param name the name of the class being loaded
	 * @param base the base class of the class being loaded
	 * @param loader the class loader to use to load the new class
	 * @return the loaded class
	 * @throws ReflectiveOperationException if the loader throws an exception while loading the bytecode
	 */
	public <T> Class<T> load(final String name, final Class<T> base, final ClassLoader loader) throws ReflectiveOperationException {
		// Add this class
		getConstant(new UTF8String(name.replace('.', '/'))).add(new Location(constantPool, constantPool.getLength() + 1));
		final ConstantPoolEntry classNameInfo = constantPool.add(this, CLASS_CONSTANT, B0, B0);

		Method method = null;
		final ConstantPoolEntry baseClassInfo;
		final ConstantPoolEntry baseConstructorInfo;
		final ConstantPoolEntry interfaceClassInfo;

		if (base.isInterface()) { // This method is implementing an interface
			for (final Method check : base.getMethods()) {
				if (!Modifier.isStatic(check.getModifiers())) {
					if (method != null) {
						throw new RuntimeException("Class " + base.getName() + " must have exactly 1 method (contains multiple)");
					}

					method = check;
				}
			}

			baseClassInfo = getConstant(Object.class);
			baseConstructorInfo = getConstant(Object.class.getDeclaredConstructor());
			interfaceClassInfo = getConstant(base);
		} else { // This method is extending a class
			for (final Method check : base.getMethods()) {
				if (Modifier.isAbstract(check.getModifiers())) {
					if (method != null) {
						throw new RuntimeException("Class " + base.getName() + " must have exactly 1 abstract method (contains multiple)");
					}

					method = check;
				}
			}

			if (method == null) {
				for (final Method check : base.getDeclaredMethods()) {
					if ((check.getModifiers() & (Modifier.FINAL | Modifier.NATIVE | Modifier.PRIVATE | Modifier.PROTECTED | Modifier.STATIC)) == 0 && !check.isSynthetic() ) {
						if (method != null) {
							throw new RuntimeException("Class " + base.getName() + " must have exactly 1 abstract method (contains none) or 1 public declared non-final, non-native, non-static method (contains multiple)");
						}

						method = check;
					}
				}

				if (method == null) {
					throw new RuntimeException("Class " + base.getName() + " must have exactly 1 abstract method or 1 public declared non-final, non-native, non-static method (contains none)");
				}
			}

			baseClassInfo = interfaceClassInfo = getConstant(base);
			baseConstructorInfo = getConstant(base.getDeclaredConstructor());
		}

		final int SUPER = 0x20; // Internal flag

		final ConstantPoolEntry ctorNameInfo = getConstant(new UTF8String("<init>"));
		final ConstantPoolEntry ctorSignatureInfo = getConstant(new UTF8String("()V"));
		final ConstantPoolEntry codeAttributeInfo = getConstant(new UTF8String("Code"));

		final ConstantPoolEntry methodNameInfo = getConstant(new UTF8String(method.getName()));
		final ConstantPoolEntry methodSignatureInfo = getConstant(new UTF8String(getSignature(method)));

		final int fieldIndex = 10 + constantPool.getLength() + (base.isInterface() ? 10 : 8); // The offset of the field start
		final int ctorIndex = fieldIndex + 4; // The offset of the constructor start
		final int methodIndex = ctorIndex + 31; // The offset of the method start
		final int attributeIndex = methodIndex + length + 26;
		final int endIndex = attributeIndex + 2;

		if (constantPool.count() >= 65536 || maxStackSize >= 65536 || maxLocalVariableIndex >= 65536) {
			throw new RuntimeException("Encountered data overflow while building method");
		}

		// Populate all labels and the constant pool
		for (final Label label : labels) {
			label.populate();
		}

		constantPool.populate();

		final byte[] classBytecode = new byte[endIndex];

		// Magic number
		classBytecode[0] = (byte)0xCA;
		classBytecode[1] = (byte)0xFE;
		classBytecode[2] = (byte)0xBA;
		classBytecode[3] = (byte)0xBE;

		// Version number
		classBytecode[4] = 0x00;
		classBytecode[5] = 0x00;
		classBytecode[6] = 0x00;
		classBytecode[7] = (byte)0x31; // Use Java 1.5, so we don't have to generate stackmaps

		// Constants count
		classBytecode[8] = (byte)(constantPool.count() >>> 8);
		classBytecode[9] = (byte)(constantPool.count());

		System.arraycopy(constantPool.getData(), 0, classBytecode, 10, constantPool.getLength());

		// Access flags (public final super)
		final int accessFlags = Modifier.PUBLIC | Modifier.FINAL | SUPER;
		classBytecode[10 + constantPool.getLength()]     = (byte)(accessFlags >>> 8);
		classBytecode[10 + constantPool.getLength() + 1] = (byte)(accessFlags);

		// This class and super class index
		classBytecode[10 + constantPool.getLength() + 2] = (byte)(classNameInfo.index >>> 8);
		classBytecode[10 + constantPool.getLength() + 3] = (byte)(classNameInfo.index);
		classBytecode[10 + constantPool.getLength() + 4] = (byte)(baseClassInfo.index >>> 8);
		classBytecode[10 + constantPool.getLength() + 5] = (byte)(baseClassInfo.index);

		// Add the interface we are implementing
		if (base.isInterface()) {
			classBytecode[10 + constantPool.getLength() + 6] = 0x00;
			classBytecode[10 + constantPool.getLength() + 7] = 0x01;
			classBytecode[10 + constantPool.getLength() + 8] = (byte)(interfaceClassInfo.index >>> 8);
			classBytecode[10 + constantPool.getLength() + 9] = (byte)(interfaceClassInfo.index);
		} else {
			classBytecode[10 + constantPool.getLength() + 6] = 0x00;
			classBytecode[10 + constantPool.getLength() + 7] = 0x00;
		}

		// No fields
		classBytecode[fieldIndex]     = 0x00;
		classBytecode[fieldIndex + 1] = 0x00;

		// Methods - constructor and interface method
		classBytecode[fieldIndex + 2] = 0x00;
		classBytecode[fieldIndex + 3] = 0x02;

		{ // Constructor
			final int ctorAccessFlags = Modifier.PUBLIC;

			classBytecode[ctorIndex]     = (byte)(ctorAccessFlags >>> 8);
			classBytecode[ctorIndex + 1] = (byte)(ctorAccessFlags);

			classBytecode[ctorIndex + 2] = (byte)(ctorNameInfo.index >>> 8);
			classBytecode[ctorIndex + 3] = (byte)(ctorNameInfo.index);
			classBytecode[ctorIndex + 4] = (byte)(ctorSignatureInfo.index >>> 8);
			classBytecode[ctorIndex + 5] = (byte)(ctorSignatureInfo.index);

			classBytecode[ctorIndex + 6] = 0x00;
			classBytecode[ctorIndex + 7] = 0x01;
			classBytecode[ctorIndex + 8] = (byte)(codeAttributeInfo.index >>> 8);
			classBytecode[ctorIndex + 9] = (byte)(codeAttributeInfo.index);

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
			classBytecode[ctorIndex + 24] = (byte)(baseConstructorInfo.index >>> 8);
			classBytecode[ctorIndex + 25] = (byte)(baseConstructorInfo.index);
			classBytecode[ctorIndex + 26] = RETURN;

			classBytecode[ctorIndex + 27] = 0x00;
			classBytecode[ctorIndex + 28] = 0x00;
			classBytecode[ctorIndex + 29] = 0x00;
			classBytecode[ctorIndex + 30] = 0x00;
		}

		int maxLocalVariableSize = maxLocalVariableIndex + 1;

		for (final Class<?> parameterType : method.getParameterTypes()) {
			maxLocalVariableSize += (double.class.equals(parameterType) || long.class.equals(parameterType) ? 2 : 1);
		}

		{ // Add the custom method
			final int methodAccessFlags = Modifier.PUBLIC | Modifier.FINAL;

			classBytecode[methodIndex]     = (byte)(methodAccessFlags >>> 8);
			classBytecode[methodIndex + 1] = (byte)(methodAccessFlags);

			classBytecode[methodIndex + 2] = (byte)(methodNameInfo.index >>> 8);
			classBytecode[methodIndex + 3] = (byte)(methodNameInfo.index);
			classBytecode[methodIndex + 4] = (byte)(methodSignatureInfo.index >>> 8);
			classBytecode[methodIndex + 5] = (byte)(methodSignatureInfo.index);

			classBytecode[methodIndex + 6] = 0x00;
			classBytecode[methodIndex + 7] = 0x01;
			classBytecode[methodIndex + 8] = (byte)(codeAttributeInfo.index >>> 8);
			classBytecode[methodIndex + 9] = (byte)(codeAttributeInfo.index);

			final int attributeLength = length + 12;

			classBytecode[methodIndex + 10] = (byte)(attributeLength >>> 24);
			classBytecode[methodIndex + 11] = (byte)(attributeLength >>> 16);
			classBytecode[methodIndex + 12] = (byte)(attributeLength >>> 8);
			classBytecode[methodIndex + 13] = (byte)(attributeLength);

			// Code
			classBytecode[methodIndex + 14] = (byte)(maxStackSize >>> 8);
			classBytecode[methodIndex + 15] = (byte)(maxStackSize);
			classBytecode[methodIndex + 16] = (byte)(maxLocalVariableSize >>> 8);
			classBytecode[methodIndex + 17] = (byte)(maxLocalVariableSize);

			classBytecode[methodIndex + 18] = (byte)(length >>> 24);
			classBytecode[methodIndex + 19] = (byte)(length >>> 16);
			classBytecode[methodIndex + 20] = (byte)(length >>> 8);
			classBytecode[methodIndex + 21] = (byte)(length);

			System.arraycopy(bytes, 0, classBytecode, methodIndex + 22, length);

			final int methodIndex2 = methodIndex + length;

			classBytecode[methodIndex2 + 22] = 0x00;
			classBytecode[methodIndex2 + 23] = 0x00;
			classBytecode[methodIndex2 + 24] = 0x00;
			classBytecode[methodIndex2 + 25] = 0x00;
		}

		// Attributes
		classBytecode[attributeIndex]     = 0x00;
		classBytecode[attributeIndex + 1] = 0x00;

		return new BytecodeLoader<T>(loader).defineClass(name, classBytecode);
	}

	/**
	 * Creates a new label targeting the current offset in the buffer.
	 *
	 * @return the new label
	 */
	public Label newLabel() {
		final Location targetLocation = new Location(this, length);

		final Label label = new Label() {
			private Location target = targetLocation;
			private final List<Location> references = new ArrayList<>();

			@Override
			public Label addReference(final MethodBuilder builder, final int offset, final int updateOffset) {
				references.add(new Location(builder, offset, updateOffset));
				builder.labels.add(this);
				return this;
			}

			@Override
			public Iterable<Location> getReferences() {
				return references;
			}

			@Override
			public Location getTarget() {
				return target;
			}

			@Override
			public Label populate() {
				for (final Location location : references) {
					final byte bytes[] = ((MethodBuilder)location.container).bytes;

					if (target.container == location.container) {
						final int branchOffset = target.offset - location.offset;

						if ((short)branchOffset != branchOffset) {
							throw new RuntimeException("Failed to generate code for a branch instruction spanning more than 32767 bytes");
						}

						bytes[location.updateOffset]     = (byte)(branchOffset >>> 8);
						bytes[location.updateOffset + 1] = (byte)(branchOffset);
					}
				}

				return this;
			}

			@Override
			public Label setTarget(final Location target) {
				this.target = target;
				return this;
			}
		};

		labels.add(label);
		return label;
	}

	/**
	 * Pushes an integer onto the stack. The value will be added to the constants pool only if there is not a more efficient way to push the value onto the stack.
	 *
	 * @param value the value to push onto the stack
	 * @return this builder
	 */
	public MethodBuilder pushConstant(final int value) {
		if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
			switch (value) {
			case -1: return addCode(ICONST_M1);
			case 0:  return addCode(ICONST_0);
			case 1:  return addCode(ICONST_1);
			case 2:  return addCode(ICONST_2);
			case 3:  return addCode(ICONST_3);
			case 4:  return addCode(ICONST_4);
			case 5:  return addCode(ICONST_5);
			default: return addCode(SIPUSH, (byte)(value >>> 8), (byte)value);
			}
		}

		getConstant(value).add(new Location(this, length + 1));
		maxStackSize = Math.max(maxStackSize, ++stackSize);
		return append(LDC_W, B0, B0);
	}

	/**
	 * Pushes a long onto the stack. The value will be added to the constants pool only if there is not a more efficient way to push the value onto the stack.
	 *
	 * @param value the value to push onto the stack
	 * @return this builder
	 */
	public MethodBuilder pushConstant(final long value) {
		if (value == 0) {
			return addCode(LCONST_0);
		} else if (value == 1) {
			return addCode(LCONST_1);
		} else if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
			return pushConstant((int)value).addCode(I2L);
		}

		getConstant(value).add(new Location(this, length + 1));
		maxStackSize = Math.max(maxStackSize, stackSize += 2);
		return append(LDC2_W, B0, B0);
	}

	/**
	 * Pushes a float onto the stack. The value will be added to the constants pool only if there is not a more efficient way to push the value onto the stack.
	 *
	 * @param value the value to push onto the stack
	 * @return this builder
	 */
	public MethodBuilder pushConstant(final float value) {
		if (value == 0.0f) {
			return addCode(FCONST_0);
		} else if (value == 1.0f) {
			return addCode(FCONST_1);
		} else if (value == 2.0f) {
			return addCode(FCONST_2);
		}

		getConstant(value).add(new Location(this, length + 1));
		maxStackSize = Math.max(maxStackSize, ++stackSize);
		return append(LDC_W, B0, B0);
	}

	/**
	 * Pushes a double onto the stack. The value will be added to the constants pool only if there is not a more efficient way to push the value onto the stack.
	 *
	 * @param value the value to push onto the stack
	 * @return this builder
	 */
	public MethodBuilder pushConstant(final double value) {
		if (value == 0.0) {
			return addCode(DCONST_0);
		} else if (value == 1.0) {
			return addCode(DCONST_1);
		} else if (value == (float)value) {
			return pushConstant((float)value).addCode(F2D);
		}

		getConstant(value).add(new Location(this, length + 1));
		maxStackSize = Math.max(maxStackSize, stackSize += 2);
		return append(LDC2_W, B0, B0);
	}

	/**
	 * Pushes a string onto the stack.
	 *
	 * @param value the value to push onto the stack
	 * @return this builder
	 */
	public MethodBuilder pushConstant(final String value) {
		getConstant(value).add(new Location(this, length + 1));
		maxStackSize = Math.max(maxStackSize, ++stackSize);
		return append(LDC_W, B0, B0);
	}

	/**
	 * Pushes a new object onto the stack.
	 *
	 * @param type the type of the object
	 * @param dimensions the dimension sizes if the object is an array
	 * @return this builder
	 */
	public MethodBuilder pushNewObject(final Class<?> type, final int... dimensions) {
		if (dimensions.length > 255) {
			throw new RuntimeException("Array dimensions to large, expecting at most 255");
		} else if (dimensions.length > 1) {
			for (final int dimension : dimensions) {
				pushConstant(dimension);
			}

			getConstant(type).add(new Location(this, length + 1));
			stackSize -= dimensions.length - 1;
			return append(MULTIANEWARRAY, B0, B0, (byte)dimensions.length);
		} else if (dimensions.length != 0) {
			pushConstant(dimensions[0]);

			if (boolean.class.equals(type)) {
				return append(NEWARRAY, (byte)4);
			} else if (char.class.equals(type)) {
				return append(NEWARRAY, (byte)5);
			} else if (float.class.equals(type)) {
				return append(NEWARRAY, (byte)6);
			} else if (double.class.equals(type)) {
				return append(NEWARRAY, (byte)7);
			} else if (byte.class.equals(type)) {
				return append(NEWARRAY, (byte)8);
			} else if (short.class.equals(type)) {
				return append(NEWARRAY, (byte)9);
			} else if (int.class.equals(type)) {
				return append(NEWARRAY, (byte)10);
			} else if (long.class.equals(type)) {
				return append(NEWARRAY, (byte)11);
			}

			getConstant(type).add(new Location(this, length + 1));
			return append(ANEWARRAY, B0, B0);
		}

		if (type.isPrimitive()) {
			throw new RuntimeException("Unexpected primitive type for new object, expecting non-primitive type");
		}

		getConstant(type).add(new Location(this, length + 1));
		maxStackSize = Math.max(maxStackSize, ++stackSize);
		return append(NEW, B0, B0);
	}

	/**
	 * Resets the builder to an empty state.
	 *
	 * @return this builder
	 */
	public MethodBuilder reset() {
		length = 0;
		maxStackSize = 0;
		stackSize = 0;
		maxLocalVariableIndex = 0;
		constantPool.clear();
		labels.clear();

		return this;
	}

	@Override
	public String toString() {
		return bytes.toString();
	}

	/**
	 * Updates the label to target the current offset in the builder.
	 *
	 * @param label the label to update
	 * @return this builder
	 */
	public MethodBuilder updateLabel(final Label label) {
		final Location target = label.getTarget();

		target.update(this, length - target.offset);
		return this;
	}

}
