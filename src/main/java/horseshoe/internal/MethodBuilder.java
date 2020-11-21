package horseshoe.internal;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

public final class MethodBuilder {

	private static final Opcode[] OPCODES = new Opcode[256];

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
	private static final byte ANEWARRAY       = new Opcode("anewarray",       0xBD, 3,  0, Opcode.PROP_CONST_POOL_INDEX).id; // 2: indexbyte1, indexbyte2 (stack: count -> arrayref)
	private static final byte CHECKCAST       = new Opcode("checkcast",       0xC0, 3,  0, Opcode.PROP_CONST_POOL_INDEX).id; // 2: indexbyte1, indexbyte2 (stack: objectref -> objectref)
	private static final byte GETFIELD        = new Opcode("getfield",        0xB4, 3,  0, Opcode.PROP_CONST_POOL_INDEX | Opcode.PROP_HAS_VARIABLE_STACK_OFFSET).id; // 2: indexbyte1, indexbyte2 (stack: objectref -> value)
	private static final byte GETSTATIC       = new Opcode("getstatic",       0xB2, 3,  0, Opcode.PROP_CONST_POOL_INDEX | Opcode.PROP_HAS_VARIABLE_STACK_OFFSET).id; // 2: indexbyte1, indexbyte2 (stack: -> value)
	private static final byte GOTO            = new Opcode("goto",            0xA7, 3,  0, Opcode.PROP_BRANCH_OFFSET | Opcode.PROP_BREAKS_FLOW).id; // 2: branchbyte1, branchbyte2 (stack: [no change])
	@SuppressWarnings("unused")
	private static final byte GOTO_W          = new Opcode("goto_w",          0xC8, 5,  0, Opcode.PROP_BRANCH_OFFSET_4 | Opcode.PROP_BREAKS_FLOW).id; // 4: branchbyte1, branchbyte2, branchbyte3, branchbyte4 (stack: [no change])
	private static final byte INSTANCEOF      = new Opcode("instanceof",      0xC1, 3,  0, Opcode.PROP_CONST_POOL_INDEX).id; // 2: indexbyte1, indexbyte2 (stack: objectref -> result)
	private static final byte INVOKEINTERFACE = new Opcode("invokeinterface", 0xB9, 5,  0, Opcode.PROP_CONST_POOL_INDEX | Opcode.PROP_HAS_CUSTOM_EXTRA_BYTES | Opcode.PROP_HAS_VARIABLE_STACK_OFFSET).id; // 4: indexbyte1, indexbyte2, count, 0 (stack: objectref, [arg1, arg2, ...] -> result)
	private static final byte INVOKESPECIAL   = new Opcode("invokespecial",   0xB7, 3,  0, Opcode.PROP_CONST_POOL_INDEX | Opcode.PROP_HAS_VARIABLE_STACK_OFFSET).id; // 2: indexbyte1, indexbyte2 (stack: objectref, [arg1, arg2, ...] -> result)
	private static final byte INVOKESTATIC    = new Opcode("invokestatic",    0xB8, 3,  0, Opcode.PROP_CONST_POOL_INDEX | Opcode.PROP_HAS_VARIABLE_STACK_OFFSET).id; // 2: indexbyte1, indexbyte2 (stack: [arg1, arg2, ...] -> result)
	private static final byte INVOKEVIRTUAL   = new Opcode("invokevirtual",   0xB6, 3,  0, Opcode.PROP_CONST_POOL_INDEX | Opcode.PROP_HAS_VARIABLE_STACK_OFFSET).id; // 2: indexbyte1, indexbyte2 (stack: objectref, [arg1, arg2, ...] -> result)
	@SuppressWarnings("unused")
	private static final byte LDC             = new Opcode("ldc",             0x12, 2,  1, Opcode.PROP_HAS_CUSTOM_EXTRA_BYTES).id; // 1: index (stack: -> value)
	private static final byte LDC_W           = new Opcode("ldc_w",           0x13, 3,  1, Opcode.PROP_CONST_POOL_INDEX).id; // 2: indexbyte1, indexbyte2 (stack: -> value)
	private static final byte LDC2_W          = new Opcode("ldc2_w",          0x14, 3,  2, Opcode.PROP_CONST_POOL_INDEX).id; // 2: indexbyte1, indexbyte2 (stack: -> value)
	private static final byte LOOKUPSWITCH    = new Opcode("lookupswitch",    0xAB, 0, -1, Opcode.PROP_HAS_CUSTOM_EXTRA_BYTES | Opcode.PROP_HAS_VARIABLE_LENGTH).id; // 8+: [0-3 bytes padding], defaultbyte1, defaultbyte2, defaultbyte3, defaultbyte4, npairs1, npairs2, npairs3, npairs4, match-offset pairs... (stack: key ->)
	private static final byte MULTIANEWARRAY  = new Opcode("multianewarray",  0xC5, 4,  0, Opcode.PROP_CONST_POOL_INDEX | Opcode.PROP_HAS_CUSTOM_EXTRA_BYTES | Opcode.PROP_HAS_VARIABLE_STACK_OFFSET).id; // 3: indexbyte1, indexbyte2, dimensions (stack: count1, [count2,...] -> arrayref)
	private static final byte NEW             = new Opcode("new",             0xBB, 3,  1, Opcode.PROP_CONST_POOL_INDEX).id; // 2: indexbyte1, indexbyte2 (stack: -> objectref)
	private static final byte PUTFIELD        = new Opcode("putfield",        0xB5, 3,  0, Opcode.PROP_CONST_POOL_INDEX | Opcode.PROP_HAS_VARIABLE_STACK_OFFSET).id; // 2: indexbyte1, indexbyte2 (stack: objectref, value ->)
	private static final byte PUTSTATIC       = new Opcode("putstatic",       0xB3, 3,  0, Opcode.PROP_CONST_POOL_INDEX | Opcode.PROP_HAS_VARIABLE_STACK_OFFSET).id; // 2: indexbyte1, indexbyte2 (stack: value ->)
	@SuppressWarnings("unused")
	private static final byte TABLESWITCH     = new Opcode("tableswitch",     0xAA, 0, -1, Opcode.PROP_HAS_CUSTOM_EXTRA_BYTES | Opcode.PROP_HAS_VARIABLE_LENGTH).id; // 16+: [0-3 bytes padding], defaultbyte1, defaultbyte2, defaultbyte3, defaultbyte4, lowbyte1, lowbyte2, lowbyte3, lowbyte4, highbyte1, highbyte2, highbyte3, highbyte4, jump offsets... (stack: index ->)

	// Do not use, added in Java 7, but we only support Java 6
	private static final byte INVOKEDYNAMIC   = new Opcode("invokedynamic",   0xBA, 5,  0, Opcode.PROP_CONST_POOL_INDEX | Opcode.PROP_HAS_CUSTOM_EXTRA_BYTES | Opcode.PROP_HAS_VARIABLE_STACK_OFFSET).id; // 4: indexbyte1, indexbyte2, 0, 0 (stack: [arg1, [arg2 ...]] -> result)

	// Instructions that can be added as code
	public static final byte AALOAD       = new Opcode("aaload",       0x32, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: arrayref, index -> value)
	public static final byte AASTORE      = new Opcode("aastore",      0x53, 1, -3, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: arrayref, index, value ->)
	public static final byte ACONST_NULL  = new Opcode("aconst_null",  0x01, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> null)
	public static final byte ALOAD        = new Opcode("aload",        0x19, 2,  1, Opcode.PROP_LOCAL_INDEX | Opcode.PROP_IS_STANDALONE_VALID).id; // 1: index (stack: -> objectref)
	public static final byte ALOAD_0      = new Opcode("aload_0",      0x2A, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> objectref)
	public static final byte ALOAD_1      = new Opcode("aload_1",      0x2B, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> objectref)
	public static final byte ALOAD_2      = new Opcode("aload_2",      0x2C, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> objectref)
	public static final byte ALOAD_3      = new Opcode("aload_3",      0x2D, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> objectref)
	public static final byte ARETURN      = new Opcode("areturn",      0xB0, 1, -1, Opcode.PROP_CLEARS_STACK | Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: objectref -> [empty])
	public static final byte ARRAYLENGTH  = new Opcode("arraylength",  0xBE, 1,  0, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: arrayref -> length)
	public static final byte ASTORE       = new Opcode("astore",       0x3A, 2, -1, Opcode.PROP_LOCAL_INDEX | Opcode.PROP_IS_STANDALONE_VALID).id; // 1: index (stack: objectref ->)
	public static final byte ASTORE_0     = new Opcode("astore_0",     0x4B, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: objectref ->)
	public static final byte ASTORE_1     = new Opcode("astore_1",     0x4C, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: objectref ->)
	public static final byte ASTORE_2     = new Opcode("astore_2",     0x4D, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: objectref ->)
	public static final byte ASTORE_3     = new Opcode("astore_3",     0x4E, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: objectref ->)
	public static final byte ATHROW       = new Opcode("athrow",       0xBF, 1, -1, Opcode.PROP_CLEARS_STACK | Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: objectref -> [empty], objectref)
	public static final byte BALOAD       = new Opcode("baload",       0x33, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: arrayref, index -> value)
	public static final byte BASTORE      = new Opcode("bastore",      0x54, 1, -3, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: arrayref, index, value ->)
	public static final byte BIPUSH       = new Opcode("bipush",       0x10, 2,  1, Opcode.PROP_HAS_CUSTOM_EXTRA_BYTES | Opcode.PROP_IS_STANDALONE_VALID).id; // 1: byte (stack: -> value)
	public static final byte BREAKPOINT   = new Opcode("breakpoint",   0xCA, 1,  0, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: )
	public static final byte CALOAD       = new Opcode("caload",       0x34, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: arrayref, index -> value)
	public static final byte CASTORE      = new Opcode("castore",      0x55, 1, -3, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: arrayref, index, value ->)
	public static final byte D2F          = new Opcode("d2f",          0x90, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value -> result)
	public static final byte D2I          = new Opcode("d2i",          0x8E, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value -> result)
	public static final byte D2L          = new Opcode("d2l",          0x8F, 1,  0, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value -> result)
	public static final byte DADD         = new Opcode("dadd",         0x63, 1, -2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte DALOAD       = new Opcode("daload",       0x31, 1,  0, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: arrayref, index -> value)
	public static final byte DASTORE      = new Opcode("dastore",      0x52, 1, -4, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: arrayref, index, value ->)
	public static final byte DCMPG        = new Opcode("dcmpg",        0x98, 1, -3, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte DCMPL        = new Opcode("dcmpl",        0x97, 1, -3, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte DCONST_0     = new Opcode("dconst_0",     0x0E, 1,  2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> 0.0)
	public static final byte DCONST_1     = new Opcode("dconst_1",     0x0F, 1,  2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> 1.0)
	public static final byte DDIV         = new Opcode("ddiv",         0x6F, 1, -2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte DLOAD        = new Opcode("dload",        0x18, 2,  2, Opcode.PROP_LOCAL_INDEX | Opcode.PROP_IS_STANDALONE_VALID).id; // 1: index (stack: -> value)
	public static final byte DLOAD_0      = new Opcode("dload_0",      0x26, 1,  2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> value)
	public static final byte DLOAD_1      = new Opcode("dload_1",      0x27, 1,  2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> value)
	public static final byte DLOAD_2      = new Opcode("dload_2",      0x28, 1,  2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> value)
	public static final byte DLOAD_3      = new Opcode("dload_3",      0x29, 1,  2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> value)
	public static final byte DMUL         = new Opcode("dmul",         0x6B, 1, -2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte DNEG         = new Opcode("dneg",         0x77, 1,  0, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value -> result)
	public static final byte DREM         = new Opcode("drem",         0x73, 1, -2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte DRETURN      = new Opcode("dreturn",      0xAF, 1, -2, Opcode.PROP_CLEARS_STACK | Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value -> [empty])
	public static final byte DSTORE       = new Opcode("dstore",       0x39, 2, -2, Opcode.PROP_LOCAL_INDEX | Opcode.PROP_IS_STANDALONE_VALID).id; // 1: index (stack: value ->)
	public static final byte DSTORE_0     = new Opcode("dstore_0",     0x47, 1, -2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value ->)
	public static final byte DSTORE_1     = new Opcode("dstore_1",     0x48, 1, -2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value ->)
	public static final byte DSTORE_2     = new Opcode("dstore_2",     0x49, 1, -2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value ->)
	public static final byte DSTORE_3     = new Opcode("dstore_3",     0x4A, 1, -2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value ->)
	public static final byte DSUB         = new Opcode("dsub",         0x67, 1, -2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte DUP          = new Opcode("dup",          0x59, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value -> value, value)
	public static final byte DUP_X1       = new Opcode("dup_x1",       0x5A, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value2, value1 -> value1, value2, value1)
	public static final byte DUP_X2       = new Opcode("dup_x2",       0x5B, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value3, value2, value1 -> value1, value3, value2, value1)
	public static final byte DUP2         = new Opcode("dup2",         0x5C, 1,  2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: {value2, value1} -> {value2, value1}, {value2, value1})
	public static final byte DUP2_X1      = new Opcode("dup2_x1",      0x5D, 1,  2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value3, {value2, value1} -> {value2, value1}, value3, {value2, value1})
	public static final byte DUP2_X2      = new Opcode("dup2_x2",      0x5E, 1,  2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: {value4, value3}, {value2, value1} -> {value2, value1}, {value4, value3}, {value2, value1})
	public static final byte F2D          = new Opcode("f2d",          0x8D, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value -> result)
	public static final byte F2I          = new Opcode("f2i",          0x8B, 1,  0, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value -> result)
	public static final byte F2L          = new Opcode("f2l",          0x8C, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value -> result)
	public static final byte FADD         = new Opcode("fadd",         0x62, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte FALOAD       = new Opcode("faload",       0x30, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: arrayref, index -> value)
	public static final byte FASTORE      = new Opcode("fastore",      0x51, 1, -3, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: arrayref, index, value ->)
	public static final byte FCMPG        = new Opcode("fcmpg",        0x96, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte FCMPL        = new Opcode("fcmpl",        0x95, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte FCONST_0     = new Opcode("fconst_0",     0x0B, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> 0.0f)
	public static final byte FCONST_1     = new Opcode("fconst_1",     0x0C, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> 1.0f)
	public static final byte FCONST_2     = new Opcode("fconst_2",     0x0D, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> 2.0f)
	public static final byte FDIV         = new Opcode("fdiv",         0x6E, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte FLOAD        = new Opcode("fload",        0x17, 2,  1, Opcode.PROP_LOCAL_INDEX | Opcode.PROP_IS_STANDALONE_VALID).id; // 1: index (stack: -> value)
	public static final byte FLOAD_0      = new Opcode("fload_0",      0x22, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> value)
	public static final byte FLOAD_1      = new Opcode("fload_1",      0x23, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> value)
	public static final byte FLOAD_2      = new Opcode("fload_2",      0x24, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> value)
	public static final byte FLOAD_3      = new Opcode("fload_3",      0x25, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> value)
	public static final byte FMUL         = new Opcode("fmul",         0x6A, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte FNEG         = new Opcode("fneg",         0x76, 1,  0, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value -> result)
	public static final byte FREM         = new Opcode("frem",         0x72, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte FRETURN      = new Opcode("freturn",      0xAE, 1, -1, Opcode.PROP_CLEARS_STACK | Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value -> [empty])
	public static final byte FSTORE       = new Opcode("fstore",       0x38, 2, -1, Opcode.PROP_LOCAL_INDEX | Opcode.PROP_IS_STANDALONE_VALID).id; // 1: index (stack: value ->)
	public static final byte FSTORE_0     = new Opcode("fstore_0",     0x43, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value ->)
	public static final byte FSTORE_1     = new Opcode("fstore_1",     0x44, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value ->)
	public static final byte FSTORE_2     = new Opcode("fstore_2",     0x45, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value ->)
	public static final byte FSTORE_3     = new Opcode("fstore_3",     0x46, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value ->)
	public static final byte FSUB         = new Opcode("fsub",         0x66, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte I2B          = new Opcode("i2b",          0x91, 1,  0, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value -> result)
	public static final byte I2C          = new Opcode("i2c",          0x92, 1,  0, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value -> result)
	public static final byte I2D          = new Opcode("i2d",          0x87, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value -> result)
	public static final byte I2F          = new Opcode("i2f",          0x86, 1,  0, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value -> result)
	public static final byte I2L          = new Opcode("i2l",          0x85, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value -> result)
	public static final byte I2S          = new Opcode("i2s",          0x93, 1,  0, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value -> result)
	public static final byte IADD         = new Opcode("iadd",         0x60, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte IALOAD       = new Opcode("iaload",       0x2E, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: arrayref, index -> value)
	public static final byte IAND         = new Opcode("iand",         0x7E, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte IASTORE      = new Opcode("iastore",      0x4F, 1, -3, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: arrayref, index, value ->)
	public static final byte ICONST_M1    = new Opcode("iconst_m1",    0x02, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> -1)
	public static final byte ICONST_0     = new Opcode("iconst_0",     0x03, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> 0)
	public static final byte ICONST_1     = new Opcode("iconst_1",     0x04, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> 1)
	public static final byte ICONST_2     = new Opcode("iconst_2",     0x05, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> 2)
	public static final byte ICONST_3     = new Opcode("iconst_3",     0x06, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> 3)
	public static final byte ICONST_4     = new Opcode("iconst_4",     0x07, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> 4)
	public static final byte ICONST_5     = new Opcode("iconst_5",     0x08, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> 5)
	public static final byte IDIV         = new Opcode("idiv",         0x6C, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte IF_ACMPEQ    = new Opcode("if_acmpeq",    0xA5, 3, -2, Opcode.PROP_BRANCH_OFFSET).id; // 2: branchbyte1, branchbyte2 (stack: value1, value2 ->)
	public static final byte IF_ACMPNE    = new Opcode("if_acmpne",    0xA6, 3, -2, Opcode.PROP_BRANCH_OFFSET).id; // 2: branchbyte1, branchbyte2 (stack: value1, value2 ->)
	public static final byte IF_ICMPEQ    = new Opcode("if_icmpeq",    0x9F, 3, -2, Opcode.PROP_BRANCH_OFFSET).id; // 2: branchbyte1, branchbyte2 (stack: value1, value2 ->)
	public static final byte IF_ICMPGE    = new Opcode("if_icmpge",    0xA2, 3, -2, Opcode.PROP_BRANCH_OFFSET).id; // 2: branchbyte1, branchbyte2 (stack: value1, value2 ->)
	public static final byte IF_ICMPGT    = new Opcode("if_icmpgt",    0xA3, 3, -2, Opcode.PROP_BRANCH_OFFSET).id; // 2: branchbyte1, branchbyte2 (stack: value1, value2 ->)
	public static final byte IF_ICMPLE    = new Opcode("if_icmple",    0xA4, 3, -2, Opcode.PROP_BRANCH_OFFSET).id; // 2: branchbyte1, branchbyte2 (stack: value1, value2 ->)
	public static final byte IF_ICMPLT    = new Opcode("if_icmplt",    0xA1, 3, -2, Opcode.PROP_BRANCH_OFFSET).id; // 2: branchbyte1, branchbyte2 (stack: value1, value2 ->)
	public static final byte IF_ICMPNE    = new Opcode("if_icmpne",    0xA0, 3, -2, Opcode.PROP_BRANCH_OFFSET).id; // 2: branchbyte1, branchbyte2 (stack: value1, value2 ->)
	public static final byte IFEQ         = new Opcode("ifeq",         0x99, 3, -1, Opcode.PROP_BRANCH_OFFSET).id; // 2: branchbyte1, branchbyte2 (stack: value ->)
	public static final byte IFGE         = new Opcode("ifge",         0x9C, 3, -1, Opcode.PROP_BRANCH_OFFSET).id; // 2: branchbyte1, branchbyte2 (stack: value ->)
	public static final byte IFGT         = new Opcode("ifgt",         0x9D, 3, -1, Opcode.PROP_BRANCH_OFFSET).id; // 2: branchbyte1, branchbyte2 (stack: value ->)
	public static final byte IFLE         = new Opcode("ifle",         0x9E, 3, -1, Opcode.PROP_BRANCH_OFFSET).id; // 2: branchbyte1, branchbyte2 (stack: value ->)
	public static final byte IFLT         = new Opcode("iflt",         0x9B, 3, -1, Opcode.PROP_BRANCH_OFFSET).id; // 2: branchbyte1, branchbyte2 (stack: value ->)
	public static final byte IFNE         = new Opcode("ifne",         0x9A, 3, -1, Opcode.PROP_BRANCH_OFFSET).id; // 2: branchbyte1, branchbyte2 (stack: value ->)
	public static final byte IFNONNULL    = new Opcode("ifnonnull",    0xC7, 3, -1, Opcode.PROP_BRANCH_OFFSET).id; // 2: branchbyte1, branchbyte2 (stack: value ->)
	public static final byte IFNULL       = new Opcode("ifnull",       0xC6, 3, -1, Opcode.PROP_BRANCH_OFFSET).id; // 2: branchbyte1, branchbyte2 (stack: value ->)
	public static final byte IINC         = new Opcode("iinc",         0x84, 3,  0, Opcode.PROP_LOCAL_INDEX | Opcode.PROP_HAS_CUSTOM_EXTRA_BYTES | Opcode.PROP_IS_STANDALONE_VALID).id; // 2: index, const (stack: [No change])
	public static final byte ILOAD        = new Opcode("iload",        0x15, 2,  1, Opcode.PROP_LOCAL_INDEX | Opcode.PROP_IS_STANDALONE_VALID).id; // 1: index (stack: -> value)
	public static final byte ILOAD_0      = new Opcode("iload_0",      0x1A, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> value)
	public static final byte ILOAD_1      = new Opcode("iload_1",      0x1B, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> value)
	public static final byte ILOAD_2      = new Opcode("iload_2",      0x1C, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> value)
	public static final byte ILOAD_3      = new Opcode("iload_3",      0x1D, 1,  1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> value)
	public static final byte IMPDEP1      = new Opcode("impdep1",      0xFE, 1,  0, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: )
	public static final byte IMPDEP2      = new Opcode("impdep2",      0xFF, 1,  0, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: )
	public static final byte IMUL         = new Opcode("imul",         0x68, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte INEG         = new Opcode("ineg",         0x74, 1,  0, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value -> result)
	public static final byte IOR          = new Opcode("ior",          0x80, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte IREM         = new Opcode("irem",         0x70, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte IRETURN      = new Opcode("ireturn",      0xAC, 1, -1, Opcode.PROP_CLEARS_STACK | Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value -> [empty])
	public static final byte ISHL         = new Opcode("ishl",         0x78, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte ISHR         = new Opcode("ishr",         0x7A, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte ISTORE       = new Opcode("istore",       0x36, 2, -1, Opcode.PROP_LOCAL_INDEX | Opcode.PROP_IS_STANDALONE_VALID).id; // 1: index (stack: value ->)
	public static final byte ISTORE_0     = new Opcode("istore_0",     0x3B, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value ->)
	public static final byte ISTORE_1     = new Opcode("istore_1",     0x3C, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value ->)
	public static final byte ISTORE_2     = new Opcode("istore_2",     0x3D, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value ->)
	public static final byte ISTORE_3     = new Opcode("istore_3",     0x3E, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value ->)
	public static final byte ISUB         = new Opcode("isub",         0x64, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte IUSHR        = new Opcode("iushr",        0x7C, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte IXOR         = new Opcode("ixor",         0x82, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte JSR          = new Opcode("jsr",          0xA8, 3,  1, Opcode.PROP_BRANCH_OFFSET).id; // 2: branchbyte1, branchbyte2 (stack: -> address)
	public static final byte JSR_W        = new Opcode("jsr_w",        0xC9, 5,  1, Opcode.PROP_BRANCH_OFFSET_4).id; // 4: branchbyte1, branchbyte2, branchbyte3, branchbyte4 (stack: -> address)
	public static final byte L2D          = new Opcode("l2d",          0x8A, 1,  0, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value -> result)
	public static final byte L2F          = new Opcode("l2f",          0x89, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value -> result)
	public static final byte L2I          = new Opcode("l2i",          0x88, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value -> result)
	public static final byte LADD         = new Opcode("ladd",         0x61, 1, -2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte LALOAD       = new Opcode("laload",       0x2F, 1,  0, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: arrayref, index -> value)
	public static final byte LAND         = new Opcode("land",         0x7F, 1, -2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte LASTORE      = new Opcode("lastore",      0x50, 1, -4, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: arrayref, index, value ->)
	public static final byte LCMP         = new Opcode("lcmp",         0x94, 1, -3, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte LCONST_0     = new Opcode("lconst_0",     0x09, 1,  2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> 0L)
	public static final byte LCONST_1     = new Opcode("lconst_1",     0x0A, 1,  2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> 1L)
	public static final byte LDIV         = new Opcode("ldiv",         0x6D, 1, -2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte LLOAD        = new Opcode("lload",        0x16, 2,  2, Opcode.PROP_LOCAL_INDEX | Opcode.PROP_IS_STANDALONE_VALID).id; // 1: index (stack: -> value)
	public static final byte LLOAD_0      = new Opcode("lload_0",      0x1E, 1,  2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> value)
	public static final byte LLOAD_1      = new Opcode("lload_1",      0x1F, 1,  2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> value)
	public static final byte LLOAD_2      = new Opcode("lload_2",      0x20, 1,  2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> value)
	public static final byte LLOAD_3      = new Opcode("lload_3",      0x21, 1,  2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> value)
	public static final byte LMUL         = new Opcode("lmul",         0x69, 1, -2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte LNEG         = new Opcode("lneg",         0x75, 1,  0, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value -> result)
	public static final byte LOR          = new Opcode("lor",          0x81, 1, -2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte LREM         = new Opcode("lrem",         0x71, 1, -2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte LRETURN      = new Opcode("lreturn",      0xAD, 1, -2, Opcode.PROP_CLEARS_STACK | Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value -> [empty])
	public static final byte LSHL         = new Opcode("lshl",         0x79, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte LSHR         = new Opcode("lshr",         0x7B, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte LSTORE       = new Opcode("lstore",       0x37, 2, -2, Opcode.PROP_LOCAL_INDEX | Opcode.PROP_IS_STANDALONE_VALID).id; // 1: index (stack: value ->)
	public static final byte LSTORE_0     = new Opcode("lstore_0",     0x3F, 1, -2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value ->)
	public static final byte LSTORE_1     = new Opcode("lstore_1",     0x40, 1, -2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value ->)
	public static final byte LSTORE_2     = new Opcode("lstore_2",     0x41, 1, -2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value ->)
	public static final byte LSTORE_3     = new Opcode("lstore_3",     0x42, 1, -2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value ->)
	public static final byte LSUB         = new Opcode("lsub",         0x65, 1, -2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte LUSHR        = new Opcode("lushr",        0x7D, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte LXOR         = new Opcode("lxor",         0x83, 1, -2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value1, value2 -> result)
	public static final byte MONITORENTER = new Opcode("monitorenter", 0xC2, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: objectref ->)
	public static final byte MONITOREXIT  = new Opcode("monitorexit",  0xC3, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: objectref ->)
	public static final byte NEWARRAY     = new Opcode("newarray",     0xBC, 2,  0, Opcode.PROP_HAS_CUSTOM_EXTRA_BYTES | Opcode.PROP_IS_STANDALONE_VALID).id; // 1: atype (stack: count -> arrayref)
	public static final byte NOP          = new Opcode("nop",          0x00, 1,  0, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: [No change])
	public static final byte POP          = new Opcode("pop",          0x57, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value ->)
	public static final byte POP2         = new Opcode("pop2",         0x58, 1, -2, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: {value2, value1} ->)
	public static final byte RET          = new Opcode("ret",          0xA9, 2,  0, Opcode.PROP_LOCAL_INDEX | Opcode.PROP_IS_STANDALONE_VALID).id; // 1: index (stack: [No change])
	public static final byte RETURN       = new Opcode("return",       0xB1, 1,  0, Opcode.PROP_CLEARS_STACK | Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: -> [empty])
	public static final byte SALOAD       = new Opcode("saload",       0x35, 1, -1, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: arrayref, index -> value)
	public static final byte SASTORE      = new Opcode("sastore",      0x56, 1, -3, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: arrayref, index, value ->)
	public static final byte SIPUSH       = new Opcode("sipush",       0x11, 3,  1, Opcode.PROP_HAS_CUSTOM_EXTRA_BYTES | Opcode.PROP_IS_STANDALONE_VALID).id; // 2: byte1, byte2 (stack: -> value)
	public static final byte SWAP         = new Opcode("swap",         0x5F, 1,  0, Opcode.PROP_IS_STANDALONE_VALID).id; // (stack: value2, value1 -> value1, value2)
	public static final byte WIDE         = new Opcode("wide",         0xC4, 0,  0, Opcode.PROP_HAS_CUSTOM_EXTRA_BYTES | Opcode.PROP_HAS_VARIABLE_LENGTH | Opcode.PROP_HAS_VARIABLE_STACK_OFFSET | Opcode.PROP_IS_STANDALONE_VALID).id; // 3/5: opcode, indexbyte1, indexbyte2 (stack: [same as for corresponding instructions])

	private byte[] bytes = new byte[256];
	private int length = 0;
	private int maxStackSize = 0;
	private int stackSize = 0;
	private int maxLocalVariableIndex = 0; // Always include index 0 to support "this" pointer to support non-static methods
	private boolean requiresAlignment = false;
	private final Set<Label> labels = new LinkedHashSet<>();

	private final ConstantPool constantPool = new ConstantPool() {
		private final Map<Object, ConstantPoolEntry> constants = new LinkedHashMap<>();
		private byte[] constantData = new byte[256];
		private int length = 0;

		@Override
		public ConstantPoolEntry add(final Object object, final byte... data) {
			final ConstantPoolEntry info = new ConstantPoolEntry((short)(constants.size() + 1));
			constants.put(object, info);

			if (length + data.length > constantData.length) {
				final byte[] newBuffer = new byte[Math.max(length + data.length, constantData.length * 2)];
				System.arraycopy(constantData, 0, newBuffer, 0, length);
				constantData = newBuffer;
			}

			System.arraycopy(data, 0, constantData, length, data.length);
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
			return constantData;
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
					final byte[] containerData = (location.container == this ? constantData : ((MethodBuilder)location.container).bytes);

					containerData[location.offset]     = (byte)(entry.index >>> 8);
					containerData[location.offset + 1] = (byte)(entry.index);
				}
			}

			return this;
		}
	};

	private static class Opcode {
		/** This value is the opcode mask for the meaning of the bytes following the opcode. */
		private static final int PROP_EXTRA_BYTES_MASK = 0x03;
		/** This value indicates the opcode has a 1 byte local variable index that follows. */
		private static final int PROP_LOCAL_INDEX = 0x01;
		/** This value indicates the opcode has a 2 byte constant pool index that follows. */
		private static final int PROP_CONST_POOL_INDEX = 0x02;
		/** This value indicates the opcode has a 2 byte branch offset that follows. */
		private static final int PROP_BRANCH_OFFSET = 0x03;

		/** This value indicates the opcode has a custom set of bytes that follow. */
		private static final int PROP_HAS_CUSTOM_EXTRA_BYTES = 0x04;
		/** This value indicates the opcode has a variable length. */
		private static final int PROP_HAS_VARIABLE_LENGTH = 0x08;
		/** This value indicates the opcode has a variable stack offset. */
		private static final int PROP_HAS_VARIABLE_STACK_OFFSET = 0x10;
		/** This value indicates the opcode breaks the flow of code, including invalidating the stack size. */
		private static final int PROP_BREAKS_FLOW = 0x20;
		/** This value indicates the opcode clears the stack. */
		private static final int PROP_CLEARS_STACK = 0x60;
		/** This value indicates the opcode is valid to use as a stand-alone instruction. */
		private static final int PROP_IS_STANDALONE_VALID = 0x80;

		/** This value indicates the opcode has a 4 byte branch offset. */
		private static final int PROP_BRANCH_OFFSET_4 = PROP_BRANCH_OFFSET | PROP_HAS_CUSTOM_EXTRA_BYTES;

		private final String mnemonic;
		private final byte id;
		private final byte length;
		private final byte stackOffset;
		private final byte properties;

		/**
		 * Creates a new opcode.
		 *
		 * @param mnemonic the string mnemonic for the opcode
		 * @param id the byte identifier for the opcode
		 * @param length the length of the opcode
		 * @param stackOffset the stack offset as a result of executing the opcode
		 * @param properties the properties of the opcode
		 */
		private Opcode(final String mnemonic, final int id, final int length, final int stackOffset, final int properties) {
			this.mnemonic = mnemonic;
			this.id = (byte)id;
			this.length = (byte)length;
			this.stackOffset = (byte)stackOffset;
			this.properties = (byte)properties;

			// Check that dynamic length is only set when custom bytes is set and
			//   length matches with expected extra bytes and
			//   length is set to zero when the length is dynamic and
			//   stack offset is set to zero when the stack offset is dynamic
			assert (properties & PROP_HAS_CUSTOM_EXTRA_BYTES) != 0 || (properties & PROP_HAS_VARIABLE_LENGTH) == 0;
			assert (properties & PROP_HAS_CUSTOM_EXTRA_BYTES) != 0 ||
					((properties & PROP_EXTRA_BYTES_MASK) == 0 && length == 1) ||
					((properties & PROP_EXTRA_BYTES_MASK) == PROP_LOCAL_INDEX && length == 2) ||
					((properties & PROP_EXTRA_BYTES_MASK) == PROP_CONST_POOL_INDEX && length == 3) ||
					((properties & PROP_EXTRA_BYTES_MASK) == PROP_BRANCH_OFFSET && length == 3);
			assert ((properties & PROP_HAS_VARIABLE_LENGTH) == 0) ^ (length == 0);
			assert (properties & PROP_HAS_VARIABLE_STACK_OFFSET) == 0 || stackOffset == 0;

			OPCODES[this.id & 0xFF] = this;
		}

		/**
		 * Checks if the opcode has any of the specified properties. The specified properties are bitwise anded with the opcodes properties and compared with zero.
		 *
		 * @param properties the properties to test
		 * @return true if the opcode has any of the specified properties, otherwise false
		 */
		private boolean has(final int properties) {
			return (this.properties & properties) != 0;
		}
	}

	private static final class Location {
		private Object container;
		private int offset;
		private int updateOffset;

		/**
		 * Creates a new location using a builder as the container.
		 *
		 * @param container the builder that contains the location
		 * @param offset the offset within the byte buffer of the container used in offset calculations
		 * @param updateOffset the offset within the byte buffer of the container updated with the appropriate information
		 */
		private Location(final MethodBuilder container, final int offset, final int updateOffset) {
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
		private Location(final MethodBuilder container, final int offset) {
			this(container, offset, offset);
		}

		/**
		 * Creates a new location using a constant pool as the container.
		 *
		 * @param container the constant pool that contains the location
		 * @param offset the offset within the byte buffer of the container
		 */
		private Location(final ConstantPool container, final int offset) {
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
		private Location update(final MethodBuilder container, final int additionalOffset) {
			this.container = container;
			this.offset += additionalOffset;
			this.updateOffset += additionalOffset;
			return this;
		}
	}

	private static final class ConstantPoolEntry {
		private final short index;
		private final List<Location> locations = new ArrayList<>();

		/**
		 * Creates a new constant pool entry.
		 *
		 * @param index the index of the entry within the constant pool
		 */
		private ConstantPoolEntry(final short index) {
			this.index = index;
		}
	}

	private static interface ConstantPool {
		/**
		 * Adds data to the constant pool. The constant pool length is automatically extended.
		 *
		 * @param object the object to add to the constant pool
		 * @param data the data to append to the constant pool
		 * @return the newly created data
		 */
		public ConstantPoolEntry add(final Object object, final byte... data);

		/**
		 * Clears the constant pool.
		 *
		 * @return this constant pool
		 */
		public ConstantPool clear();

		/**
		 * Gets the constant pool count.
		 *
		 * @return the constant pool count
		 */
		public int count();

		/**
		 * Gets data from the constant pool.
		 *
		 * @param object the object to lookup in the constant pool
		 * @return the data in the constant pool, or null if it does not exist
		 */
		public ConstantPoolEntry get(final Object object);

		/**
		 * Gets the byte data for the constant pool.
		 *
		 * @return the byte data for the constant pool
		 */
		public byte[] getData();

		/**
		 * Gets the set of all entries in the constant pool.
		 *
		 * @return the set of all entries in the constant pool
		 */
		public Set<Entry<Object, ConstantPoolEntry>> getEntries();

		/**
		 * Gets the length of the constant pool.
		 *
		 * @return the length of the constant pool
		 */
		abstract int getLength();

		/**
		 * Populates the data dependent on the set of constants in the constant pool.
		 *
		 * @return this constant pool
		 */
		public ConstantPool populate();
	}

	public static class Label {
		private final Location target;
		private final List<Location> references = new ArrayList<>();

		/**
		 * Creates a new label with the specified target.
		 *
		 * @param target the target location for the label
		 */
		private Label(final Location target) {
			this.target = target;
		}

		/**
		 * Adds a reference to the label.
		 *
		 * @param builder the builder that references the label
		 * @param offset the offset of the reference within the builder
		 * @param updateOffset the offset within the builder that gets updated with the branch offset
		 * @return this label
		 */
		private Label addReference(final MethodBuilder builder, final int offset, final int updateOffset) {
			references.add(new Location(builder, offset, updateOffset));
			builder.labels.add(this);
			return this;
		}

		/**
		 * Gets an iterator to all references to the label.
		 *
		 * @return an iterator to all references to the label
		 */
		private Iterable<Location> getReferences() {
			return references;
		}

		/**
		 * Gets the target for the label.
		 *
		 * @return the target for the label
		 */
		private Location getTarget() {
			return target;
		}

		/**
		 * Populates the data dependent on this label.
		 *
		 * @return this label
		 */
		private Label populate() {
			for (final Location location : references) {
				final byte[] bytes = ((MethodBuilder)location.container).bytes;

				if (target.container == location.container) {
					final int branchOffset = target.offset - location.offset;

					if ((short)branchOffset != branchOffset) {
						throw new IllegalStateException("Failed to generate code for a branch instruction spanning more than 32767 bytes");
					}

					bytes[location.updateOffset]     = (byte)(branchOffset >>> 8);
					bytes[location.updateOffset + 1] = (byte)(branchOffset);
				}
			}

			return this;
		}
	}

	private static final class UTF8String {
		private final String value;

		private UTF8String(final String value) {
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
		private BytecodeLoader(final ClassLoader parent) {
			super(parent);
		}

		@SuppressWarnings("unchecked")
		public Class<U> defineClass(final String name, final byte[] bytecode) {
			return (Class<U>)super.defineClass(name, bytecode, 0, bytecode.length);
		}
	}

	/**
	 * Adds an access (load / store) instruction. This is a convenience method that applies the "wide" instruction prefix if needed.
	 *
	 * @param instruction the load or store instruction
	 * @param index the index of the local variable to access
	 * @return this builder
	 */
	public MethodBuilder addAccess(final byte instruction, final int index) {
		if (index >> 16 != 0) {
			throw new IllegalArgumentException("Access index out of range (" + index + "), expecting 0 - 65535");
		}

		return index >= 256 ? addCode(WIDE, instruction, (byte)(index >>> 8), (byte)index) : addCode(instruction, (byte)index);
	}

	/**
	 * Adds a branch instruction to the given label. Note that the label must be in the same buffer or it must be combined with the buffer containing the label at some point.
	 *
	 * @param instruction the branch instruction
	 * @param label the label to branch to
	 * @return this builder
	 */
	public MethodBuilder addBranch(final byte instruction, final Label label) {
		final Opcode opcode = OPCODES[instruction & 0xFF];

		if (opcode == null || !opcode.has(Opcode.PROP_BRANCH_OFFSET)) {
			final String mnemonic = (opcode == null ? "0x" + Integer.toHexString(instruction & 0xFF) : opcode.mnemonic);
			throw new IllegalArgumentException("Unexpected bytecode instruction: " + mnemonic + ", expecting a branch instruction");
		} else if (opcode.has(Opcode.PROP_BREAKS_FLOW)) {
			throw new IllegalArgumentException("Illegal branch instruction " + opcode.mnemonic + ", instruction cannot be added as a branch instruction");
		}

		label.addReference(this, length, length + 1);

		if (instruction == JSR_W) {
			stackSize++;
			maxStackSize = Math.max(maxStackSize, stackSize);
			return append(JSR, B0, B0);
		}

		stackSize += opcode.stackOffset;
		maxStackSize = Math.max(maxStackSize, stackSize);
		return append(instruction, B0, B0);
	}

	/**
	 * Adds a cast to the specified type.
	 *
	 * @param type the type to cast to
	 * @return this builder
	 */
	public MethodBuilder addCast(final Class<?> type) {
		getConstant(type).locations.add(new Location(this, length + 1));
		return append(CHECKCAST, B0, B0);
	}

	/**
	 * Adds code to the builder, reserving extra space in the builder if necessary. The length is automatically extended. Any exception will leave the builder in a valid state where all code up to the opcode that caused the exception has been added to the builder.
	 *
	 * @param code the code to add to the builder
	 * @return this builder
	 */
	public MethodBuilder addCode(final byte... code) {
		int i = 0;

		try {
			while (i < code.length) {
				i += addCode(code, i);
			}
		} finally {
			append(code, i);
		}

		return this;
	}

	/**
	 * Adds the specified opcode at the starting index in the code array to the builder. The code is not actually copied
	 *
	 * @param code the code array containing the opcode to add to the builder
	 * @param start the starting index of the opcode within the code array
	 * @return the length of the opcode, in bytes
	 */
	private int addCode(final byte[] code, final int start) {
		final Opcode opcode = OPCODES[code[start] & 0xFF];

		if (opcode == null) {
			throw new IllegalArgumentException("Invalid bytecode instruction 0x" + Integer.toHexString(code[start] & 0xFF) + " at index " + start);
		} else if (!opcode.has(Opcode.PROP_IS_STANDALONE_VALID) || opcode.has(Opcode.PROP_BREAKS_FLOW)) {
			throw new IllegalArgumentException("Illegal bytecode instruction " + opcode.mnemonic + " at index " + start + ", instruction cannot be added directly");
		} else if (code[start] == WIDE) {
			if (start + 1 >= code.length) {
				throw new IllegalArgumentException("Incomplete bytecode instruction " + opcode.mnemonic + " at index " + start);
			}

			final Opcode wideOpcode = OPCODES[code[start + 1] & 0xFF];
			final int opcodeLength;

			if (wideOpcode == null) {
				throw new IllegalArgumentException("Invalid bytecode instruction " + opcode.mnemonic + " 0x" + Integer.toHexString(code[start + 1] & 0xFF) + " at index " + start);
			} else if (!wideOpcode.has(Opcode.PROP_LOCAL_INDEX)) {
				throw new IllegalArgumentException("Invalid bytecode instruction " + opcode.mnemonic + " " + wideOpcode.mnemonic + " at index " + start);
			} else if (code[start + 1] == IINC) {
				opcodeLength = 6;
			} else {
				opcodeLength = 4;
			}

			if (start + opcodeLength > code.length) {
				throw new IllegalArgumentException("Incomplete bytecode instruction " + opcode.mnemonic + " " + wideOpcode.mnemonic + " at index " + start);
			}

			maxLocalVariableIndex = Math.max(maxLocalVariableIndex, ((code[start + 2] & 0xFF) << 8) + (code[start + 3] & 0xFF));
			stackSize += wideOpcode.stackOffset;
			maxStackSize = Math.max(maxStackSize, stackSize);
			return opcodeLength;
		} else if (opcode.has(Opcode.PROP_LOCAL_INDEX)) {
			maxLocalVariableIndex = Math.max(maxLocalVariableIndex, code[start + 1] & 0xFF);
		}

		stackSize += opcode.stackOffset;
		maxStackSize = Math.max(maxStackSize, stackSize);
		return opcode.length;
	}

	/**
	 * Adds code to access (load or store) the specified field.
	 *
	 * @param field the field to access
	 * @param load true to load the value of the field, false to store the value into the field
	 * @return this builder
	 */
	public MethodBuilder addFieldAccess(final Field field, final boolean load) {
		getConstant(field).locations.add(new Location(this, length + 1));

		if (Modifier.isStatic(field.getModifiers())) {
			if (load) {
				stackSize += getStackSize(field.getType());
				maxStackSize = Math.max(maxStackSize, stackSize);
				return append(GETSTATIC, B0, B0);
			}

			stackSize -= getStackSize(field.getType());
			return append(PUTSTATIC, B0, B0);
		} else if (load) {
			stackSize += getStackSize(field.getType()) - 1;
			maxStackSize = Math.max(maxStackSize, stackSize);
			return append(GETFIELD, B0, B0);
		}

		stackSize -= getStackSize(field.getType()) + 1;
		return append(PUTFIELD, B0, B0);
	}

	/**
	 * Adds flow-breaking code to the builder, reserving extra space in the builder if necessary. The length is automatically extended. Any exception will leave the builder in a valid state without the opcode added.
	 *
	 * @param code the code to add to the builder
	 * @param stackPop the number of times the stack should be popped after the instruction, assuming it did not break the flow of the code
	 * @return this builder
	 */
	public MethodBuilder addFlowBreakingCode(final byte code, final int stackPop) {
		final Opcode opcode = OPCODES[code & 0xFF];

		if (opcode == null) {
			throw new IllegalArgumentException("Invalid bytecode instruction 0x" + Integer.toHexString(code & 0xFF));
		} else if (!opcode.has(Opcode.PROP_IS_STANDALONE_VALID) || !opcode.has(Opcode.PROP_BREAKS_FLOW)) {
			throw new IllegalArgumentException("Illegal bytecode instruction " + opcode.mnemonic + ", instruction cannot be added directly");
		}

		append(new byte[] { code }, 1);
		stackSize = stackSize + opcode.stackOffset - stackPop;
		maxStackSize = Math.max(maxStackSize, stackSize);
		return this;
	}

	/**
	 * Adds a goto to the given label. Note that the label must be in the same buffer or it must be combined with the buffer containing the label at some point.
	 *
	 * @param label the label to branch to
	 * @param stackPop the number of times the stack should be popped after the goto, assuming it did not break the flow of the code
	 * @return this builder
	 */
	public MethodBuilder addGoto(final Label label, final int stackPop) {
		label.addReference(this, length, length + 1);
		stackSize -= stackPop;
		maxStackSize = Math.max(maxStackSize, stackSize);
		return append(GOTO, B0, B0);
	}

	/**
	 * Adds an instanceof check.
	 *
	 * @param type the type to check
	 * @return this builder
	 */
	public MethodBuilder addInstanceOfCheck(final Class<?> type) {
		getConstant(type).locations.add(new Location(this, length + 1));
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
		getConstant(method).locations.add(new Location(this, length + 1));
		final int modifiers = method.getModifiers();
		final int methodStackOffset = getCallStackOffset(method.getReturnType(), method.getParameterTypes());

		if (Modifier.isStatic(modifiers)) {
			stackSize += methodStackOffset;
			maxStackSize = Math.max(maxStackSize, stackSize);
			return append(INVOKESTATIC, B0, B0);
		} else if (Modifier.isInterface(method.getDeclaringClass().getModifiers())) {
			final int stackOffset = methodStackOffset - 1;
			stackSize += stackOffset;
			maxStackSize = Math.max(maxStackSize, stackSize);
			return append(INVOKEINTERFACE, B0, B0, (byte)(getStackSize(method.getReturnType()) - stackOffset), B0);
		} else if (Modifier.isPrivate(modifiers) || superCall) {
			stackSize += methodStackOffset - 1;
			maxStackSize = Math.max(maxStackSize, stackSize);
			return append(INVOKESPECIAL, B0, B0);
		}

		stackSize += methodStackOffset - 1;
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
		getConstant(constructor).locations.add(new Location(this, length + 1));

		stackSize += getCallStackOffset(void.class, constructor.getParameterTypes()) - 1;
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
			if (to.isAssignableFrom(from)) {
				return this;
			} else if (!from.isPrimitive()) { // Step 1: Unbox if needed
				if (Character.class.equals(from)) {
					return addInvoke(Character.class.getMethod("charValue")).addPrimitiveConversion(char.class, to);
				} else if (Boolean.class.equals(from)) {
					return addInvoke(Boolean.class.getMethod("booleanValue")).addPrimitiveConversion(boolean.class, to);
				} else if (Number.class.isAssignableFrom(from)) {
					if (boolean.class.equals(to) || Boolean.class.equals(to)) {
						return addCode(DUP).addInvoke(Number.class.getMethod("doubleValue")).addCode(DCONST_0, DCMPG, SWAP).addInvoke(Number.class.getMethod("longValue")).addCode(DUP2).pushConstant(32).addCode(LUSHR, L2I, DUP_X2, POP, L2I, IOR, IOR).addPrimitiveConversion(int.class, to);
					} else if (char.class.equals(to) || Character.class.equals(to) || int.class.equals(to) || Integer.class.equals(to)) {
						return addInvoke(Number.class.getMethod("intValue")).addPrimitiveConversion(int.class, to);
					} else if (long.class.equals(to) || Long.class.equals(to)) {
						return addInvoke(Number.class.getMethod("longValue")).addPrimitiveConversion(long.class, to);
					} else if (double.class.equals(to) || Double.class.equals(to)) {
						return addInvoke(Number.class.getMethod("doubleValue")).addPrimitiveConversion(double.class, to);
					} else if (float.class.equals(to) || Float.class.equals(to)) {
						return addInvoke(Number.class.getMethod("floatValue")).addPrimitiveConversion(float.class, to);
					} else if (short.class.equals(to) || Short.class.equals(to)) {
						return addInvoke(Number.class.getMethod("shortValue")).addPrimitiveConversion(short.class, to);
					} else if (byte.class.equals(to) || Byte.class.equals(to)) {
						return addInvoke(Number.class.getMethod("byteValue")).addPrimitiveConversion(byte.class, to);
					}
				}
			} else if (to.isPrimitive()) { // Step 2: Convert primitive if needed
				if (boolean.class.equals(from) || char.class.equals(from) || int.class.equals(from) || short.class.equals(from) || byte.class.equals(from)) {
					if (boolean.class.equals(to) || int.class.equals(to)) {
						return this;
					} else if (short.class.equals(to)) {
						return addCode(I2S);
					} else if (byte.class.equals(to)) {
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
					if (char.class.equals(to) || int.class.equals(to) || short.class.equals(to) || byte.class.equals(to)) {
						return addCode(L2I).addPrimitiveConversion(int.class, to);
					} else if (float.class.equals(to)) {
						return addCode(L2F);
					} else if (double.class.equals(to)) {
						return addCode(L2D);
					} else if (boolean.class.equals(to)) {
						return addCode(LCONST_0, LCMP);
					}
				} else if (float.class.equals(from)) {
					if (char.class.equals(to) || int.class.equals(to) || short.class.equals(to) || byte.class.equals(to)) {
						return addCode(F2I).addPrimitiveConversion(int.class, to);
					} else if (long.class.equals(to)) {
						return addCode(F2L);
					} else if (double.class.equals(to)) {
						return addCode(F2D);
					} else if (boolean.class.equals(to)) {
						return addCode(FCONST_0, FCMPG);
					}
				} else if (double.class.equals(from)) {
					if (char.class.equals(to) || int.class.equals(to) || short.class.equals(to) || byte.class.equals(to)) {
						return addCode(D2I).addPrimitiveConversion(int.class, to);
					} else if (long.class.equals(to)) {
						return addCode(D2L);
					} else if (float.class.equals(to)) {
						return addCode(D2F);
					} else if (boolean.class.equals(to)) {
						return addCode(DCONST_0, DCMPG);
					}
				}
			} else { // Step 3: Box primitive if needed
				if (Boolean.class.equals(to)) {
					return addPrimitiveConversion(from, boolean.class).addInvoke(Boolean.class.getMethod("valueOf", boolean.class));
				} else if (Character.class.equals(to)) {
					return addPrimitiveConversion(from, char.class).addInvoke(Character.class.getMethod("valueOf", char.class));
				} else if (Integer.class.equals(to)) {
					return addPrimitiveConversion(from, int.class).addInvoke(Integer.class.getMethod("valueOf", int.class));
				} else if (Long.class.equals(to)) {
					return addPrimitiveConversion(from, long.class).addInvoke(Long.class.getMethod("valueOf", long.class));
				} else if (Double.class.equals(to)) {
					return addPrimitiveConversion(from, double.class).addInvoke(Double.class.getMethod("valueOf", double.class));
				} else if (Float.class.equals(to)) {
					return addPrimitiveConversion(from, float.class).addInvoke(Float.class.getMethod("valueOf", float.class));
				} else if (Short.class.equals(to)) {
					return addPrimitiveConversion(from, short.class).addInvoke(Short.class.getMethod("valueOf", short.class));
				} else if (Byte.class.equals(to)) {
					return addPrimitiveConversion(from, byte.class).addInvoke(Byte.class.getMethod("valueOf", byte.class));
				} else if (Object.class.equals(to) || Number.class.equals(to)) {
					if (Object.class.equals(to) && boolean.class.equals(from)) {
						return addInvoke(Boolean.class.getMethod("valueOf", boolean.class));
					} else if (Object.class.equals(to) && char.class.equals(from)) {
						return addInvoke(Character.class.getMethod("valueOf", char.class));
					} else if (int.class.equals(from)) {
						return addInvoke(Integer.class.getMethod("valueOf", int.class));
					} else if (long.class.equals(from)) {
						return addInvoke(Long.class.getMethod("valueOf", long.class));
					} else if (double.class.equals(from)) {
						return addInvoke(Double.class.getMethod("valueOf", double.class));
					} else if (float.class.equals(from)) {
						return addInvoke(Float.class.getMethod("valueOf", float.class));
					} else if (short.class.equals(from)) {
						return addInvoke(Short.class.getMethod("valueOf", short.class));
					} else if (byte.class.equals(from)) {
						return addInvoke(Byte.class.getMethod("valueOf", byte.class));
					} else if (boolean.class.equals(from)) {
						return addInvoke(Integer.class.getMethod("valueOf", int.class));
					} else if (char.class.equals(from)) {
						return addInvoke(Integer.class.getMethod("valueOf", int.class));
					}
				}
			}
		} catch (final ReflectiveOperationException e) {
			throw new NoSuchMethodError("Failed to get required class member: " + e.getMessage());
		}

		throw new IllegalArgumentException("The \"from\" type (" + from.getName() + ") must be convertible to the \"to\" type (" + to.getName() + ")");
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

		requiresAlignment = true;

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
	 * @param stackPop the number of times the stack should be popped after the throw, assuming it did not break the flow of the code
	 * @return this builder
	 */
	public MethodBuilder addThrow(final Class<? extends Throwable> throwable, final String message, final int stackPop) {
		try {
			if (message == null) {
				return pushNewObject(throwable).addCode(DUP).addInvoke(throwable.getConstructor()).addFlowBreakingCode(ATHROW, stackPop);
			}

			return pushNewObject(throwable).addCode(DUP).pushConstant(message).addInvoke(throwable.getConstructor(String.class)).addFlowBreakingCode(ATHROW, stackPop);
		} catch (final ReflectiveOperationException e) {
			throw new IncompatibleClassChangeError("Failed to get required class constructor: " + e.getMessage());
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
		if (other.requiresAlignment) {
			requiresAlignment = true;

			while (length % 4 != 0) {
				append(NOP);
			}
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
				if (location.container != other) {
					continue;
				} else if (newLocations == null) {
					newLocations = getConstant(entry.getKey()).locations;
				}

				newLocations.add(location.update(this, oldLength));
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
	 * Builds the method by creating the bytecode of a new class containing the method.
	 *
	 * @param name the name of the new class
	 * @param base the base class or interface of the new class
	 * @param isStatic true to generate a static method, false to generate an instance method
	 * @param methodName the constant pool entry for the method name
	 * @param returnType the return type of the method
	 * @param parameterTypes the parameter types of the method
	 * @return the bytecode of the new class
	 * @throws ReflectiveOperationException if an exception is thrown while generating the bytecode
	 */
	private byte[] build(final String name, final Class<?> base, final boolean isStatic, final String methodName, final Class<?> returnType, final Class<?>... parameterTypes) throws ReflectiveOperationException {
		if (stackSize != 0) {
			throw new IllegalStateException("Stack size is not zero: " + stackSize);
		}

		// Add this class
		getConstant(new UTF8String(name.replace('.', '/'))).locations.add(new Location(constantPool, constantPool.getLength() + 1));
		final ConstantPoolEntry classNameInfo = constantPool.add(this, CLASS_CONSTANT, B0, B0);
		final ConstantPoolEntry codeAttributeInfo = getConstant(new UTF8String("Code"));
		final ConstantPoolEntry methodNameInfo = getConstant(new UTF8String(methodName));
		final ConstantPoolEntry methodSignatureInfo = getConstant(new UTF8String(getSignature(returnType, parameterTypes)));

		ConstantPoolEntry ctorNameInfo = null;
		ConstantPoolEntry ctorSignatureInfo = null;

		if (!isStatic) {
			ctorNameInfo = getConstant(new UTF8String("<init>"));
			ctorSignatureInfo = getConstant(new UTF8String("()V"));
		}

		final ConstantPoolEntry baseClassInfo;
		final ConstantPoolEntry baseConstructorInfo;
		ConstantPoolEntry interfaceClassInfo = null;
		final int fieldIndex; // The offset of the field start

		if (base.isInterface()) {
			baseClassInfo = getConstant(Object.class);
			baseConstructorInfo = getConstant(Object.class.getDeclaredConstructor());
			interfaceClassInfo = getConstant(base);
			fieldIndex = constantPool.getLength() + 20;
		} else {
			baseClassInfo = getConstant(base);
			baseConstructorInfo = getConstant(base.getDeclaredConstructor());
			fieldIndex = constantPool.getLength() + 18;
		}

		final int ctorIndex = fieldIndex + 4; // The offset of the constructor start
		final int methodIndex = (isStatic ? ctorIndex : ctorIndex + 31); // The offset of the method start
		final int attributeIndex = methodIndex + length + 26;
		final int endIndex = attributeIndex + 2;

		if (length >= 65536 || constantPool.count() >= 65536 || maxStackSize >= 65536 || maxLocalVariableIndex >= 65536) {
			throw new IllegalStateException("Encountered overflow while building method");
		}

		// Populate all labels and the constant pool
		for (final Label label : labels) {
			label.populate();
		}

		constantPool.populate();

		final byte[] bytecode = new byte[endIndex];

		// Magic number
		bytecode[0] = (byte)0xCA;
		bytecode[1] = (byte)0xFE;
		bytecode[2] = (byte)0xBA;
		bytecode[3] = (byte)0xBE;

		// Version number [4 - 6] = 0
		bytecode[7] = (byte)0x31; // Use Java 1.5, so we don't have to generate stackmaps

		// Constants count
		bytecode[8] = (byte)(constantPool.count() >>> 8);
		bytecode[9] = (byte)(constantPool.count());

		System.arraycopy(constantPool.getData(), 0, bytecode, 10, constantPool.getLength());

		// Access flags (public final super)
		final int SUPER = 0x20; // Internal flag
		final int accessFlags = Modifier.PUBLIC | Modifier.FINAL | SUPER;
		bytecode[10 + constantPool.getLength()]     = (byte)(accessFlags >>> 8);
		bytecode[10 + constantPool.getLength() + 1] = (byte)(accessFlags);

		// This class and super class index
		bytecode[10 + constantPool.getLength() + 2] = (byte)(classNameInfo.index >>> 8);
		bytecode[10 + constantPool.getLength() + 3] = (byte)(classNameInfo.index);
		bytecode[10 + constantPool.getLength() + 4] = (byte)(baseClassInfo.index >>> 8);
		bytecode[10 + constantPool.getLength() + 5] = (byte)(baseClassInfo.index);

		// Add the interface we are implementing (ignore if class; [6 - 7] = 0)
		if (base.isInterface()) {
			bytecode[10 + constantPool.getLength() + 7] = 1; // [6] = 0
			bytecode[10 + constantPool.getLength() + 8] = (byte)(interfaceClassInfo.index >>> 8);
			bytecode[10 + constantPool.getLength() + 9] = (byte)(interfaceClassInfo.index);
		}

		// Fields (none) [0 - 1] = 0

		// Methods [2] = 0
		if (isStatic) { // Only 1 method
			bytecode[fieldIndex + 3] = 1;
		} else { // Constructor + method = 2 methods
			bytecode[fieldIndex + 3] = 2;

			final int ctorAccessFlags = Modifier.PUBLIC;

			bytecode[ctorIndex]     = (byte)(ctorAccessFlags >>> 8);
			bytecode[ctorIndex + 1] = (byte)(ctorAccessFlags);

			bytecode[ctorIndex + 2] = (byte)(ctorNameInfo.index >>> 8);
			bytecode[ctorIndex + 3] = (byte)(ctorNameInfo.index);
			bytecode[ctorIndex + 4] = (byte)(ctorSignatureInfo.index >>> 8);
			bytecode[ctorIndex + 5] = (byte)(ctorSignatureInfo.index);

			bytecode[ctorIndex + 7] = 0x01; // [6] = 0
			bytecode[ctorIndex + 8] = (byte)(codeAttributeInfo.index >>> 8);
			bytecode[ctorIndex + 9] = (byte)(codeAttributeInfo.index);

			bytecode[ctorIndex + 13] = 0x11; // [10 - 12] = 0

			// Code
			bytecode[ctorIndex + 15] = 0x01; // [14] = 0
			bytecode[ctorIndex + 17] = 0x01; // [16] = 0

			bytecode[ctorIndex + 21] = 0x05; // [18 - 20] = 0

			bytecode[ctorIndex + 22] = ALOAD_0;
			bytecode[ctorIndex + 23] = INVOKESPECIAL;
			bytecode[ctorIndex + 24] = (byte)(baseConstructorInfo.index >>> 8);
			bytecode[ctorIndex + 25] = (byte)(baseConstructorInfo.index);
			bytecode[ctorIndex + 26] = RETURN;

			// [27 - 30] = 0
		}

		int maxLocalVariableSize = maxLocalVariableIndex + 1;

		for (final Class<?> parameterType : parameterTypes) {
			maxLocalVariableSize += getStackSize(parameterType);
		}

		{ // Add the custom method
			final int methodAccessFlags = isStatic ?
					Modifier.PUBLIC | Modifier.STATIC :
					Modifier.PUBLIC | Modifier.FINAL;

			bytecode[methodIndex]     = (byte)(methodAccessFlags >>> 8);
			bytecode[methodIndex + 1] = (byte)(methodAccessFlags);

			bytecode[methodIndex + 2] = (byte)(methodNameInfo.index >>> 8);
			bytecode[methodIndex + 3] = (byte)(methodNameInfo.index);
			bytecode[methodIndex + 4] = (byte)(methodSignatureInfo.index >>> 8);
			bytecode[methodIndex + 5] = (byte)(methodSignatureInfo.index);

			bytecode[methodIndex + 7] = 0x01; // [6] = 0
			bytecode[methodIndex + 8] = (byte)(codeAttributeInfo.index >>> 8);
			bytecode[methodIndex + 9] = (byte)(codeAttributeInfo.index);

			final int attributeLength = length + 12;

			bytecode[methodIndex + 10] = (byte)(attributeLength >>> 24);
			bytecode[methodIndex + 11] = (byte)(attributeLength >>> 16);
			bytecode[methodIndex + 12] = (byte)(attributeLength >>> 8);
			bytecode[methodIndex + 13] = (byte)(attributeLength);

			// Code
			bytecode[methodIndex + 14] = (byte)(maxStackSize >>> 8);
			bytecode[methodIndex + 15] = (byte)(maxStackSize);
			bytecode[methodIndex + 16] = (byte)(maxLocalVariableSize >>> 8);
			bytecode[methodIndex + 17] = (byte)(maxLocalVariableSize);

			bytecode[methodIndex + 18] = (byte)(length >>> 24);
			bytecode[methodIndex + 19] = (byte)(length >>> 16);
			bytecode[methodIndex + 20] = (byte)(length >>> 8);
			bytecode[methodIndex + 21] = (byte)(length);

			System.arraycopy(bytes, 0, bytecode, methodIndex + 22, length);
		}

		// Attributes [0 - 1] = 0

		return bytecode;
	}

	/**
	 * Builds the method by loading the bytecode of the method builder into a new class.
	 *
	 * @param <T> the type of the base class
	 * @param name the name of the new class
	 * @param base the base class of the new class
	 * @param loader the class loader used to load the new class
	 * @return the new class
	 * @throws ReflectiveOperationException if an exception is thrown while loading the bytecode
	 */
	public <T> Class<T> build(final String name, final Class<T> base, final ClassLoader loader) throws ReflectiveOperationException {
		Method method = null;

		if (base.isInterface()) { // This method is implementing an interface
			for (final Method check : base.getMethods()) {
				if (check.isSynthetic()) {
					continue;
				} else if (method != null) {
					throw new IllegalArgumentException("Base interface " + base.getName() + " must have exactly 1 non-static method (contains multiple)");
				}

				method = check;
			}
		} else if (Modifier.isFinal(base.getModifiers())) { // This method is extending a class
			throw new IllegalArgumentException("Base class " + base.getName() + " must not be marked final");
		} else {
			for (final Method check : base.getMethods()) {
				if (!Modifier.isAbstract(check.getModifiers())) {
					continue;
				} else if (method != null) {
					throw new IllegalArgumentException("Base class " + base.getName() + " must have exactly 1 abstract method (contains multiple)");
				}

				method = check;
			}

			if (method == null) {
				for (final Method check : base.getDeclaredMethods()) {
					if ((check.getModifiers() & (Modifier.FINAL | Modifier.NATIVE | Modifier.PRIVATE | Modifier.PROTECTED | Modifier.STATIC)) != 0 || check.isSynthetic()) {
						continue;
					} else if (method != null) {
						throw new IllegalArgumentException("Base class " + base.getName() + " must have exactly 1 abstract method or 1 public declared non-static, non-final, non-native method (contains multiple)");
					}

					method = check;
				}
			}
		}

		if (method == null) {
			throw new IllegalArgumentException("Base class " + base.getName() + " must have exactly 1 abstract method or 1 public declared non-static, non-final, non-native method (contains none)");
		}

		final byte[] bytecode = build(name, base, false, method.getName(), method.getReturnType(), method.getParameterTypes());

		return AccessController.doPrivileged(
			new PrivilegedAction<Class<T>>() {
				@Override
				public Class<T> run() {
					return new BytecodeLoader<T>(loader).defineClass(name, bytecode);
				}
			}
		);
	}

	/**
	 * Builds the method by loading the bytecode of the method builder into a new class.
	 *
	 * @param name the name of the new class
	 * @param methodName the name of the method
	 * @param type the type signature of the method
	 * @param loader the class loader used to load the new class
	 * @return the created method
	 * @throws ReflectiveOperationException if an exception is thrown while loading the bytecode
	 */
	public Method build(final String name, final String methodName, final MethodType type, final ClassLoader loader) throws ReflectiveOperationException {
		final byte[] bytecode = build(name, Object.class, true, methodName, type.returnType(), type.parameterArray());

		return AccessController.doPrivileged(
			new PrivilegedAction<Method>() {
				@Override
				public Method run() {
					return new BytecodeLoader<Object>(loader).defineClass(name, bytecode).getMethods()[0];
				}
			}
		);
	}

	/**
	 * Gets the stack offset that results from invoking a method or constructor with the given return type and parameter types. The object for non-static members is not factored into the stack offset.
	 *
	 * @param returnType the return type of the method (void for constructors)
	 * @param parameterTypes the parameter types of the method
	 * @return the stack offset that results from invoking the method or constructor
	 */
	private static int getCallStackOffset(final Class<?> returnType, final Class<?>... parameterTypes) {
		int offset = getStackSize(returnType);

		for (final Class<?> type : parameterTypes) {
			offset -= getStackSize(type);
		}

		return offset;
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
		} else if (value instanceof Integer) {
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
			final String string = value.toString();

			try (final ByteArrayOutputStream os = new ByteArrayOutputStream(3 + string.length() * 3);
					final DataOutputStream ds = new DataOutputStream(os)) {
				ds.writeByte(STRING_CONSTANT);
				ds.writeUTF(string); // Writes the length of the string as the first 2 bytes, followed by the modified UTF-8 version of the string

				return constantPool.add(value, os.toByteArray());
			} catch (final IOException e) {
				throw new IllegalArgumentException("Failed to add string to constant pool", e);
			}
		} else if (value instanceof String) {
			getConstant(new UTF8String(value.toString())).locations.add(new Location(constantPool, constantPool.getLength() + 1));

			return constantPool.add(value, STRING_REF_CONSTANT, B0, B0);
		} else if (value instanceof Class) {
			getConstant(new UTF8String(((Class<?>)value).getName().replace('.', '/'))).locations.add(new Location(constantPool, constantPool.getLength() + 1));

			return constantPool.add(value, CLASS_CONSTANT, B0, B0);
		} else if (!(value instanceof Member)) {
			throw new IllegalArgumentException("Cannot add a constant of type " + value.getClass().toString());
		}

		// Build the signature
		final Member member = (Member)value;
		final ConstantPoolEntry name = getConstant(new UTF8String(member instanceof Constructor ? "<init>" : member.getName()));
		final ConstantPoolEntry signature = getConstant(new UTF8String(getSignature(member)));
		final ConstantPoolEntry declaringClass = getConstant(member.getDeclaringClass());

		name.locations.add(new Location(constantPool, constantPool.getLength() + 1));
		signature.locations.add(new Location(constantPool, constantPool.getLength() + 3));
		constantPool.add(new Object(), NAME_AND_TYPE_CONSTANT, B0, B0, B0, B0).locations.add(new Location(constantPool, constantPool.getLength() + 3));
		declaringClass.locations.add(new Location(constantPool, constantPool.getLength() + 1));

		if (member instanceof Field) { // Field
			return constantPool.add(member, FIELD_CONSTANT, B0, B0, B0, B0);
		} else if ((member.getDeclaringClass().getModifiers() & Modifier.INTERFACE) != 0) { // Interface method
			return constantPool.add(member, IMETHOD_CONSTANT, B0, B0, B0, B0);
		}

		return constantPool.add(member, METHOD_CONSTANT, B0, B0, B0, B0);
	}

	/**
	 * Gets the string signature of a member (constructor or method).
	 *
	 * @param member the member used to create the signature
	 * @return the signature of the member
	 */
	private static String getSignature(final Member member) {
		if (member instanceof Method) {
			return getSignature(((Method)member).getReturnType(), ((Method)member).getParameterTypes());
		} else if (member instanceof Constructor) {
			return getSignature(void.class, ((Constructor<?>)member).getParameterTypes());
		}

		// Assume field
		assert member instanceof Field : "Unknown member type " + member.getClass().getName();
		return Array.newInstance(((Field)member).getType(), 0).getClass().getName().substring(1).replace('.', '/');
	}

	/**
	 * Gets the string signature given a return type and parameter types.
	 *
	 * @param returnType the return type of the method (void for constructors)
	 * @param parameterTypes the parameter types of the method
	 * @return the string signature
	 */
	private static String getSignature(final Class<?> returnType, final Class<?>... parameterTypes) {
		final StringBuilder sb = new StringBuilder("(");

		for (final Class<?> type : parameterTypes) {
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
	 * Gets the stack size of the specified type.
	 *
	 * @param type the type to evaluate
	 * @return the stack size of the specified type
	 */
	private static int getStackSize(final Class<?> type) {
		if (type.equals(double.class) || type.equals(long.class)) {
			return 2;
		} else if (type.equals(void.class)) {
			return 0;
		}

		return 1;
	}

	/**
	 * Creates a new label targeting the current offset in the buffer.
	 *
	 * @return the new label
	 */
	public Label newLabel() {
		final Label label = new Label(new Location(this, length));

		labels.add(label);
		return label;
	}

	/**
	 * Pushes a boolean onto the stack.
	 *
	 * @param value the value to push onto the stack
	 * @return this builder
	 */
	public MethodBuilder pushConstant(final boolean value) {
		return addCode(value ? ICONST_1 : ICONST_0);
	}

	/**
	 * Pushes an integer onto the stack. The value will be added to the constants pool only if there is not a more efficient way to push the value onto the stack.
	 *
	 * @param value the value to push onto the stack
	 * @return this builder
	 */
	public MethodBuilder pushConstant(final int value) {
		switch (value) {
			case -1: return addCode(ICONST_M1);
			case 0:  return addCode(ICONST_0);
			case 1:  return addCode(ICONST_1);
			case 2:  return addCode(ICONST_2);
			case 3:  return addCode(ICONST_3);
			case 4:  return addCode(ICONST_4);
			case 5:  return addCode(ICONST_5);
			default:
				if (value == (byte)value) {
					return addCode(BIPUSH, (byte)value);
				} else if (value == (short)value) {
					return addCode(SIPUSH, (byte)(value >>> 8), (byte)value);
				}

				break;
		}

		getConstant(value).locations.add(new Location(this, length + 1));
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
		} else if (value == (int)value) {
			return pushConstant((int)value).addCode(I2L);
		}

		getConstant(value).locations.add(new Location(this, length + 1));
		stackSize += 2;
		maxStackSize = Math.max(maxStackSize, stackSize);
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

		getConstant(value).locations.add(new Location(this, length + 1));
		stackSize++;
		maxStackSize = Math.max(maxStackSize, stackSize);
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

		getConstant(value).locations.add(new Location(this, length + 1));
		stackSize += 2;
		maxStackSize = Math.max(maxStackSize, stackSize);
		return append(LDC2_W, B0, B0);
	}

	/**
	 * Pushes a string onto the stack.
	 *
	 * @param value the value to push onto the stack
	 * @return this builder
	 */
	public MethodBuilder pushConstant(final String value) {
		getConstant(value).locations.add(new Location(this, length + 1));
		stackSize++;
		maxStackSize = Math.max(maxStackSize, stackSize);
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
			throw new IllegalArgumentException("Array dimensions to large, expecting at most 255");
		} else if (dimensions.length > 1) {
			for (final int dimension : dimensions) {
				pushConstant(dimension);
			}

			getConstant(Array.newInstance(type, new int[dimensions.length]).getClass()).locations.add(new Location(this, length + 1));
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

			getConstant(type).locations.add(new Location(this, length + 1));
			return append(ANEWARRAY, B0, B0);
		}

		if (type.isPrimitive()) {
			throw new IllegalArgumentException("Unexpected primitive type for new object, expecting non-primitive type");
		}

		getConstant(type).locations.add(new Location(this, length + 1));
		stackSize++;
		maxStackSize = Math.max(maxStackSize, stackSize);
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
		if (length == 0) {
			return "";
		}

		// Populate all labels and the constant pool
		for (final Label label : labels) {
			label.populate();
		}

		final Object[] constantPoolArray = new Object[this.constantPool.populate().getEntries().size() + 1];

		for (final Entry<Object, ConstantPoolEntry> entry : this.constantPool.getEntries()) {
			constantPoolArray[entry.getValue().index] = entry.getKey();
		}

		final StringBuilder sb = new StringBuilder("[ ");

		// Loop through each instruction (assumes each instruction is already valid and the length is correct)
		for (int i = 0; i < length; i += toString(sb, constantPoolArray, bytes, i));

		sb.setLength(sb.length() - 2);
		return sb.append(" ]").toString();
	}

	/**
	 * Converts the opcode at the specified starting index to a string. The string is appended to the string builder. The string always starts with the starting index + ": " and ends with "; ", so the strings can easily be concatenated.
	 *
	 * @param sb the string builder to use to write the opcode string
	 * @param constantPoolArray the array used to look up constant pool values
	 * @param code the code array containing the opcode to write as a string
	 * @param start the starting index of the opcode within the code array
	 * @return the length of the opcode, in bytes
	 */
	private static int toString(final StringBuilder sb, final Object[] constantPoolArray, final byte[] code, final int start) {
		final Opcode opcode = OPCODES[code[start] & 0xFF];

		assert opcode != null : "Invalid opcode instruction 0x" + Integer.toHexString(code[start] & 0xFF);
		sb.append(start).append(": ").append(opcode.mnemonic);

		if (!opcode.has(Opcode.PROP_HAS_CUSTOM_EXTRA_BYTES | Opcode.PROP_HAS_VARIABLE_LENGTH)) {
			switch (opcode.properties & Opcode.PROP_EXTRA_BYTES_MASK) {
				case Opcode.PROP_LOCAL_INDEX: // 2-byte opcode with index
					sb.append(' ').append(code[start + 1] & 0xFF).append("; ");
					break;
				case Opcode.PROP_CONST_POOL_INDEX: // 3-byte opcode with constant pool index
					toStringConstant(sb, constantPoolArray[((code[start + 1] & 0xFF) << 8) + (code[start + 2] & 0xFF)], "");
					break;
				case Opcode.PROP_BRANCH_OFFSET: // 3-byte opcode with branch offset
					sb.append(' ').append(start + ((code[start + 1] & 0xFF) << 8) + (code[start + 2] & 0xFF)).append("; ");
					break;
				default: // 1-byte opcode
					sb.append("; ");
					break;
			}
		} else if (code[start] == INVOKEINTERFACE || code[start] == INVOKEDYNAMIC) {
			toStringConstant(sb, constantPoolArray[((code[start + 1] & 0xFF) << 8) + (code[start + 2] & 0xFF)], "");
		} else if (code[start] == LOOKUPSWITCH) {
			final int j = (start + 3) & ~3;
			final int npairs = (code[j + 4] << 24) + ((code[j + 5] & 0xFF) << 16) + ((code[j + 6] & 0xFF) << 8) + (code[j + 7] & 0xFF);

			sb.append(' ').append(start + (code[j] << 24) + ((code[j + 1] & 0xFF) << 16) + ((code[j + 2] & 0xFF) << 8) + (code[j + 3] & 0xFF));

			for (int k = 1; k <= npairs; k++) {
				sb.append(", ").append((code[j + k * 8] << 24) + ((code[j + k * 8 + 1] & 0xFF) << 16) + ((code[j + k * 8 + 2] & 0xFF) << 8) + (code[j + k * 8 + 3] & 0xFF)).append(" -> ").append(start + (code[j + k * 8 + 4] << 24) + ((code[j + k * 8 + 5] & 0xFF) << 16) + ((code[j + k * 8 + 6] & 0xFF) << 8) + (code[j + k * 8 + 7] & 0xFF));
			}

			sb.append("; ");
			return j + 8 + npairs * 8;
		} else if (code[start] == MULTIANEWARRAY) {
			toStringConstant(sb, constantPoolArray[((code[start + 1] & 0xFF) << 8) + (code[start + 2] & 0xFF)], ", " + (code[start + 3] & 0xFF));
		} else if (code[start] == BIPUSH || code[start] == NEWARRAY) {
			sb.append(' ').append(code[start + 1]).append("; ");
		} else if (code[start] == IINC) {
			sb.append(' ').append(code[start + 1] & 0xFF).append(", ").append(code[start + 2]).append("; ");
		} else if (code[start] == SIPUSH) {
			sb.append(' ').append((short)(code[start + 1] << 8) + (code[start + 2] & 0xFF)).append("; ");
		} else if (code[start] == WIDE) {
			final Opcode wideOpcode = OPCODES[code[start + 1] & 0xFF];

			if (code[start + 1] == IINC) {
				sb.append(' ').append(wideOpcode.mnemonic).append(' ').append(((code[start + 2] & 0xFF) << 8) + (code[start + 3] & 0xFF)).append(", ").append((short)(code[start + 4] << 8) + (code[start + 5] & 0xFF)).append("; ");
				return 6;
			}

			assert wideOpcode.has(Opcode.PROP_LOCAL_INDEX) : "Invalid wide opcode detected: " + wideOpcode == null ? "0x" + Integer.toHexString(code[start] & 0xFF) : wideOpcode.mnemonic;
			sb.append(' ').append(wideOpcode.mnemonic).append(' ').append(((code[start + 2] & 0xFF) << 8) + (code[start + 3] & 0xFF)).append("; ");
			return 4;
		} else {
			throw new IllegalStateException("Variable length opcode missing to string conversion: " + opcode.mnemonic);
		}

		assert opcode.length > 0 : "Invalid opcode length (" + opcode.length + "): " + opcode.mnemonic;
		return opcode.length;
	}

	/**
	 * Appends a constant pool value to a string builder.
	 *
	 * @param sb the string builder to use to write the constant pool value
	 * @param constant the constant pool value to append to the string builder
	 * @param extra the code array containing the opcode to write as a string
	 */
	private static void toStringConstant(final StringBuilder sb, final Object constant, final String extra) {
		if (constant instanceof String) {
			sb.append(" \"").append(constant).append(extra).append("\"; ");
		} else if (constant instanceof Member) {
			if (constant instanceof Method) {
				String separator = "";
				sb.append(' ').append(((Method)constant).getDeclaringClass().getName()).append('.').append(((Method)constant).getName()).append('(');

				for (final Class<?> parameterType : ((Method)constant).getParameterTypes()) {
					sb.append(separator).append(parameterType.getName());
					separator = ", ";
				}

				sb.append(')').append(extra).append("; ");
			} else {
				sb.append(' ').append(((Member)constant).getDeclaringClass().getName()).append('.').append(((Member)constant).getName()).append(extra).append("; ");
			}
		} else if (constant instanceof Class) {
			sb.append(' ').append(((Class<?>)constant).getName()).append(extra).append("; ");
		} else {
			sb.append(' ').append(constant).append(extra).append("; ");
		}
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
