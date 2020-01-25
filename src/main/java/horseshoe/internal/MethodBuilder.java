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

	private static final OpCode OPCODES[] = new OpCode[256];

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
	private static final byte ANEWARRAY       = new OpCode("anewarray",       0xBD, 3,  0, OpCode.PROP_CONST_POOL_INDEX).opcode; // 2: indexbyte1, indexbyte2 (stack: count -> arrayref)
	private static final byte CHECKCAST       = new OpCode("checkcast",       0xC0, 3,  0, OpCode.PROP_CONST_POOL_INDEX).opcode; // 2: indexbyte1, indexbyte2 (stack: objectref -> objectref)
	private static final byte GETFIELD        = new OpCode("getfield",        0xB4, 3,  0, OpCode.PROP_CONST_POOL_INDEX | OpCode.PROP_HAS_VARIABLE_STACK_OFFSET).opcode; // 2: indexbyte1, indexbyte2 (stack: objectref -> value)
	private static final byte GETSTATIC       = new OpCode("getstatic",       0xB2, 3,  0, OpCode.PROP_CONST_POOL_INDEX | OpCode.PROP_HAS_VARIABLE_STACK_OFFSET).opcode; // 2: indexbyte1, indexbyte2 (stack: -> value)
	private static final byte INSTANCEOF      = new OpCode("instanceof",      0xC1, 3,  0, OpCode.PROP_CONST_POOL_INDEX).opcode; // 2: indexbyte1, indexbyte2 (stack: objectref -> result)
	private static final byte INVOKEINTERFACE = new OpCode("invokeinterface", 0xB9, 5,  0, OpCode.PROP_CONST_POOL_INDEX | OpCode.PROP_HAS_CUSTOM_EXTRA_BYTES | OpCode.PROP_HAS_VARIABLE_STACK_OFFSET).opcode; // 4: indexbyte1, indexbyte2, count, 0 (stack: objectref, [arg1, arg2, ...] -> result)
	private static final byte INVOKESPECIAL   = new OpCode("invokespecial",   0xB7, 3,  0, OpCode.PROP_CONST_POOL_INDEX | OpCode.PROP_HAS_VARIABLE_STACK_OFFSET).opcode; // 2: indexbyte1, indexbyte2 (stack: objectref, [arg1, arg2, ...] -> result)
	private static final byte INVOKESTATIC    = new OpCode("invokestatic",    0xB8, 3,  0, OpCode.PROP_CONST_POOL_INDEX | OpCode.PROP_HAS_VARIABLE_STACK_OFFSET).opcode; // 2: indexbyte1, indexbyte2 (stack: [arg1, arg2, ...] -> result)
	private static final byte INVOKEVIRTUAL   = new OpCode("invokevirtual",   0xB6, 3,  0, OpCode.PROP_CONST_POOL_INDEX | OpCode.PROP_HAS_VARIABLE_STACK_OFFSET).opcode; // 2: indexbyte1, indexbyte2 (stack: objectref, [arg1, arg2, ...] -> result)
	private static final byte LDC             = new OpCode("ldc",             0x12, 2,  1, OpCode.PROP_HAS_CUSTOM_EXTRA_BYTES).opcode; // 1: index (stack: -> value)
	private static final byte LDC_W           = new OpCode("ldc_w",           0x13, 3,  1, OpCode.PROP_CONST_POOL_INDEX).opcode; // 2: indexbyte1, indexbyte2 (stack: -> value)
	private static final byte LDC2_W          = new OpCode("ldc2_w",          0x14, 3,  2, OpCode.PROP_CONST_POOL_INDEX).opcode; // 2: indexbyte1, indexbyte2 (stack: -> value)
	private static final byte LOOKUPSWITCH    = new OpCode("lookupswitch",    0xAB, 0, -1, OpCode.PROP_HAS_CUSTOM_EXTRA_BYTES | OpCode.PROP_HAS_VARIABLE_LENGTH).opcode; // 8+: [0-3 bytes padding], defaultbyte1, defaultbyte2, defaultbyte3, defaultbyte4, npairs1, npairs2, npairs3, npairs4, match-offset pairs... (stack: key ->)
	private static final byte MULTIANEWARRAY  = new OpCode("multianewarray",  0xC5, 4,  0, OpCode.PROP_CONST_POOL_INDEX | OpCode.PROP_HAS_CUSTOM_EXTRA_BYTES | OpCode.PROP_HAS_VARIABLE_STACK_OFFSET).opcode; // 3: indexbyte1, indexbyte2, dimensions (stack: count1, [count2,...] -> arrayref)
	private static final byte NEW             = new OpCode("new",             0xBB, 3,  1, OpCode.PROP_CONST_POOL_INDEX).opcode; // 2: indexbyte1, indexbyte2 (stack: -> objectref)
	private static final byte PUTFIELD        = new OpCode("putfield",        0xB5, 3,  0, OpCode.PROP_CONST_POOL_INDEX | OpCode.PROP_HAS_VARIABLE_STACK_OFFSET).opcode; // 2: indexbyte1, indexbyte2 (stack: objectref, value ->)
	private static final byte PUTSTATIC       = new OpCode("putstatic",       0xB3, 3,  0, OpCode.PROP_CONST_POOL_INDEX | OpCode.PROP_HAS_VARIABLE_STACK_OFFSET).opcode; // 2: indexbyte1, indexbyte2 (stack: value ->)
	private static final byte TABLESWITCH     = new OpCode("tableswitch",     0xAA, 0, -1, OpCode.PROP_HAS_CUSTOM_EXTRA_BYTES | OpCode.PROP_HAS_VARIABLE_LENGTH).opcode; // 16+: [0-3 bytes padding], defaultbyte1, defaultbyte2, defaultbyte3, defaultbyte4, lowbyte1, lowbyte2, lowbyte3, lowbyte4, highbyte1, highbyte2, highbyte3, highbyte4, jump offsets... (stack: index ->)

	// Do not use, added in Java 7, but we only support Java 6
	private static final byte INVOKEDYNAMIC   = new OpCode("invokedynamic",   0xBA, 5,  0, OpCode.PROP_CONST_POOL_INDEX | OpCode.PROP_HAS_CUSTOM_EXTRA_BYTES | OpCode.PROP_HAS_VARIABLE_STACK_OFFSET).opcode; // 4: indexbyte1, indexbyte2, 0, 0 (stack: [arg1, [arg2 ...]] -> result)

	// Instructions that can be added as code
	public static final byte AALOAD       = new OpCode("aaload",       0x32, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: arrayref, index -> value)
	public static final byte AASTORE      = new OpCode("aastore",      0x53, 1, -3, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: arrayref, index, value ->)
	public static final byte ACONST_NULL  = new OpCode("aconst_null",  0x01, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> null)
	public static final byte ALOAD        = new OpCode("aload",        0x19, 2,  1, OpCode.PROP_LOCAL_INDEX | OpCode.PROP_IS_STANDALONE_VALID).opcode; // 1: index (stack: -> objectref)
	public static final byte ALOAD_0      = new OpCode("aload_0",      0x2A, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> objectref)
	public static final byte ALOAD_1      = new OpCode("aload_1",      0x2B, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> objectref)
	public static final byte ALOAD_2      = new OpCode("aload_2",      0x2C, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> objectref)
	public static final byte ALOAD_3      = new OpCode("aload_3",      0x2D, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> objectref)
	public static final byte ARETURN      = new OpCode("areturn",      0xB0, 1,  0, OpCode.PROP_HAS_VARIABLE_STACK_OFFSET | OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: objectref -> [empty])
	public static final byte ARRAYLENGTH  = new OpCode("arraylength",  0xBE, 1,  0, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: arrayref -> length)
	public static final byte ASTORE       = new OpCode("astore",       0x3A, 2, -1, OpCode.PROP_LOCAL_INDEX | OpCode.PROP_IS_STANDALONE_VALID).opcode; // 1: index (stack: objectref ->)
	public static final byte ASTORE_0     = new OpCode("astore_0",     0x4B, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: objectref ->)
	public static final byte ASTORE_1     = new OpCode("astore_1",     0x4C, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: objectref ->)
	public static final byte ASTORE_2     = new OpCode("astore_2",     0x4D, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: objectref ->)
	public static final byte ASTORE_3     = new OpCode("astore_3",     0x4E, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: objectref ->)
	public static final byte ATHROW       = new OpCode("athrow",       0xBF, 1,  0, OpCode.PROP_HAS_VARIABLE_STACK_OFFSET | OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: objectref -> [empty], objectref)
	public static final byte BALOAD       = new OpCode("baload",       0x33, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: arrayref, index -> value)
	public static final byte BASTORE      = new OpCode("bastore",      0x54, 1, -3, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: arrayref, index, value ->)
	public static final byte BIPUSH       = new OpCode("bipush",       0x10, 2,  1, OpCode.PROP_HAS_CUSTOM_EXTRA_BYTES | OpCode.PROP_IS_STANDALONE_VALID).opcode; // 1: byte (stack: -> value)
	public static final byte BREAKPOINT   = new OpCode("breakpoint",   0xCA, 1,  0, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: )
	public static final byte CALOAD       = new OpCode("caload",       0x34, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: arrayref, index -> value)
	public static final byte CASTORE      = new OpCode("castore",      0x55, 1, -3, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: arrayref, index, value ->)
	public static final byte D2F          = new OpCode("d2f",          0x90, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value -> result)
	public static final byte D2I          = new OpCode("d2i",          0x8E, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value -> result)
	public static final byte D2L          = new OpCode("d2l",          0x8F, 1,  0, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value -> result)
	public static final byte DADD         = new OpCode("dadd",         0x63, 1, -2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte DALOAD       = new OpCode("daload",       0x31, 1,  0, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: arrayref, index -> value)
	public static final byte DASTORE      = new OpCode("dastore",      0x52, 1, -4, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: arrayref, index, value ->)
	public static final byte DCMPG        = new OpCode("dcmpg",        0x98, 1, -3, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte DCMPL        = new OpCode("dcmpl",        0x97, 1, -3, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte DCONST_0     = new OpCode("dconst_0",     0x0E, 1,  2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> 0.0)
	public static final byte DCONST_1     = new OpCode("dconst_1",     0x0F, 1,  2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> 1.0)
	public static final byte DDIV         = new OpCode("ddiv",         0x6F, 1, -2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte DLOAD        = new OpCode("dload",        0x18, 2,  2, OpCode.PROP_LOCAL_INDEX | OpCode.PROP_IS_STANDALONE_VALID).opcode; // 1: index (stack: -> value)
	public static final byte DLOAD_0      = new OpCode("dload_0",      0x26, 1,  2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> value)
	public static final byte DLOAD_1      = new OpCode("dload_1",      0x27, 1,  2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> value)
	public static final byte DLOAD_2      = new OpCode("dload_2",      0x28, 1,  2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> value)
	public static final byte DLOAD_3      = new OpCode("dload_3",      0x29, 1,  2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> value)
	public static final byte DMUL         = new OpCode("dmul",         0x6B, 1, -2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte DNEG         = new OpCode("dneg",         0x77, 1,  0, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value -> result)
	public static final byte DREM         = new OpCode("drem",         0x73, 1, -2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte DRETURN      = new OpCode("dreturn",      0xAF, 1,  0, OpCode.PROP_HAS_VARIABLE_STACK_OFFSET | OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value -> [empty])
	public static final byte DSTORE       = new OpCode("dstore",       0x39, 2, -2, OpCode.PROP_LOCAL_INDEX | OpCode.PROP_IS_STANDALONE_VALID).opcode; // 1: index (stack: value ->)
	public static final byte DSTORE_0     = new OpCode("dstore_0",     0x47, 1, -2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value ->)
	public static final byte DSTORE_1     = new OpCode("dstore_1",     0x48, 1, -2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value ->)
	public static final byte DSTORE_2     = new OpCode("dstore_2",     0x49, 1, -2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value ->)
	public static final byte DSTORE_3     = new OpCode("dstore_3",     0x4A, 1, -2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value ->)
	public static final byte DSUB         = new OpCode("dsub",         0x67, 1, -2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte DUP          = new OpCode("dup",          0x59, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value -> value, value)
	public static final byte DUP_X1       = new OpCode("dup_x1",       0x5A, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value2, value1 -> value1, value2, value1)
	public static final byte DUP_X2       = new OpCode("dup_x2",       0x5B, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value3, value2, value1 -> value1, value3, value2, value1)
	public static final byte DUP2         = new OpCode("dup2",         0x5C, 1,  2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: {value2, value1} -> {value2, value1}, {value2, value1})
	public static final byte DUP2_X1      = new OpCode("dup2_x1",      0x5D, 1,  2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value3, {value2, value1} -> {value2, value1}, value3, {value2, value1})
	public static final byte DUP2_X2      = new OpCode("dup2_x2",      0x5E, 1,  2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: {value4, value3}, {value2, value1} -> {value2, value1}, {value4, value3}, {value2, value1})
	public static final byte F2D          = new OpCode("f2d",          0x8D, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value -> result)
	public static final byte F2I          = new OpCode("f2i",          0x8B, 1,  0, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value -> result)
	public static final byte F2L          = new OpCode("f2l",          0x8C, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value -> result)
	public static final byte FADD         = new OpCode("fadd",         0x62, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte FALOAD       = new OpCode("faload",       0x30, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: arrayref, index -> value)
	public static final byte FASTORE      = new OpCode("fastore",      0x51, 1, -3, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: arrayref, index, value ->)
	public static final byte FCMPG        = new OpCode("fcmpg",        0x96, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte FCMPL        = new OpCode("fcmpl",        0x95, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte FCONST_0     = new OpCode("fconst_0",     0x0B, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> 0.0f)
	public static final byte FCONST_1     = new OpCode("fconst_1",     0x0C, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> 1.0f)
	public static final byte FCONST_2     = new OpCode("fconst_2",     0x0D, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> 2.0f)
	public static final byte FDIV         = new OpCode("fdiv",         0x6E, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte FLOAD        = new OpCode("fload",        0x17, 2,  1, OpCode.PROP_LOCAL_INDEX | OpCode.PROP_IS_STANDALONE_VALID).opcode; // 1: index (stack: -> value)
	public static final byte FLOAD_0      = new OpCode("fload_0",      0x22, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> value)
	public static final byte FLOAD_1      = new OpCode("fload_1",      0x23, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> value)
	public static final byte FLOAD_2      = new OpCode("fload_2",      0x24, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> value)
	public static final byte FLOAD_3      = new OpCode("fload_3",      0x25, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> value)
	public static final byte FMUL         = new OpCode("fmul",         0x6A, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte FNEG         = new OpCode("fneg",         0x76, 1,  0, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value -> result)
	public static final byte FREM         = new OpCode("frem",         0x72, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte FRETURN      = new OpCode("freturn",      0xAE, 1,  0, OpCode.PROP_HAS_VARIABLE_STACK_OFFSET | OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value -> [empty])
	public static final byte FSTORE       = new OpCode("fstore",       0x38, 2, -1, OpCode.PROP_LOCAL_INDEX | OpCode.PROP_IS_STANDALONE_VALID).opcode; // 1: index (stack: value ->)
	public static final byte FSTORE_0     = new OpCode("fstore_0",     0x43, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value ->)
	public static final byte FSTORE_1     = new OpCode("fstore_1",     0x44, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value ->)
	public static final byte FSTORE_2     = new OpCode("fstore_2",     0x45, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value ->)
	public static final byte FSTORE_3     = new OpCode("fstore_3",     0x46, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value ->)
	public static final byte FSUB         = new OpCode("fsub",         0x66, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte GOTO         = new OpCode("goto",         0xA7, 3,  0, OpCode.PROP_BRANCH_OFFSET).opcode; // 2: branchbyte1, branchbyte2 (stack: [no change])
	public static final byte GOTO_W       = new OpCode("goto_w",       0xC8, 5,  0, OpCode.PROP_BRANCH_OFFSET_4).opcode; // 4: branchbyte1, branchbyte2, branchbyte3, branchbyte4 (stack: [no change])
	public static final byte I2B          = new OpCode("i2b",          0x91, 1,  0, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value -> result)
	public static final byte I2C          = new OpCode("i2c",          0x92, 1,  0, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value -> result)
	public static final byte I2D          = new OpCode("i2d",          0x87, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value -> result)
	public static final byte I2F          = new OpCode("i2f",          0x86, 1,  0, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value -> result)
	public static final byte I2L          = new OpCode("i2l",          0x85, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value -> result)
	public static final byte I2S          = new OpCode("i2s",          0x93, 1,  0, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value -> result)
	public static final byte IADD         = new OpCode("iadd",         0x60, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte IALOAD       = new OpCode("iaload",       0x2E, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: arrayref, index -> value)
	public static final byte IAND         = new OpCode("iand",         0x7E, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte IASTORE      = new OpCode("iastore",      0x4F, 1, -3, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: arrayref, index, value ->)
	public static final byte ICONST_M1    = new OpCode("iconst_m1",    0x02, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> -1)
	public static final byte ICONST_0     = new OpCode("iconst_0",     0x03, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> 0)
	public static final byte ICONST_1     = new OpCode("iconst_1",     0x04, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> 1)
	public static final byte ICONST_2     = new OpCode("iconst_2",     0x05, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> 2)
	public static final byte ICONST_3     = new OpCode("iconst_3",     0x06, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> 3)
	public static final byte ICONST_4     = new OpCode("iconst_4",     0x07, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> 4)
	public static final byte ICONST_5     = new OpCode("iconst_5",     0x08, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> 5)
	public static final byte IDIV         = new OpCode("idiv",         0x6C, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte IF_ACMPEQ    = new OpCode("if_acmpeq",    0xA5, 3, -2, OpCode.PROP_BRANCH_OFFSET).opcode; // 2: branchbyte1, branchbyte2 (stack: value1, value2 ->)
	public static final byte IF_ACMPNE    = new OpCode("if_acmpne",    0xA6, 3, -2, OpCode.PROP_BRANCH_OFFSET).opcode; // 2: branchbyte1, branchbyte2 (stack: value1, value2 ->)
	public static final byte IF_ICMPEQ    = new OpCode("if_icmpeq",    0x9F, 3, -2, OpCode.PROP_BRANCH_OFFSET).opcode; // 2: branchbyte1, branchbyte2 (stack: value1, value2 ->)
	public static final byte IF_ICMPGE    = new OpCode("if_icmpge",    0xA2, 3, -2, OpCode.PROP_BRANCH_OFFSET).opcode; // 2: branchbyte1, branchbyte2 (stack: value1, value2 ->)
	public static final byte IF_ICMPGT    = new OpCode("if_icmpgt",    0xA3, 3, -2, OpCode.PROP_BRANCH_OFFSET).opcode; // 2: branchbyte1, branchbyte2 (stack: value1, value2 ->)
	public static final byte IF_ICMPLE    = new OpCode("if_icmple",    0xA4, 3, -2, OpCode.PROP_BRANCH_OFFSET).opcode; // 2: branchbyte1, branchbyte2 (stack: value1, value2 ->)
	public static final byte IF_ICMPLT    = new OpCode("if_icmplt",    0xA1, 3, -2, OpCode.PROP_BRANCH_OFFSET).opcode; // 2: branchbyte1, branchbyte2 (stack: value1, value2 ->)
	public static final byte IF_ICMPNE    = new OpCode("if_icmpne",    0xA0, 3, -2, OpCode.PROP_BRANCH_OFFSET).opcode; // 2: branchbyte1, branchbyte2 (stack: value1, value2 ->)
	public static final byte IFEQ         = new OpCode("ifeq",         0x99, 3, -1, OpCode.PROP_BRANCH_OFFSET).opcode; // 2: branchbyte1, branchbyte2 (stack: value ->)
	public static final byte IFGE         = new OpCode("ifge",         0x9C, 3, -1, OpCode.PROP_BRANCH_OFFSET).opcode; // 2: branchbyte1, branchbyte2 (stack: value ->)
	public static final byte IFGT         = new OpCode("ifgt",         0x9D, 3, -1, OpCode.PROP_BRANCH_OFFSET).opcode; // 2: branchbyte1, branchbyte2 (stack: value ->)
	public static final byte IFLE         = new OpCode("ifle",         0x9E, 3, -1, OpCode.PROP_BRANCH_OFFSET).opcode; // 2: branchbyte1, branchbyte2 (stack: value ->)
	public static final byte IFLT         = new OpCode("iflt",         0x9B, 3, -1, OpCode.PROP_BRANCH_OFFSET).opcode; // 2: branchbyte1, branchbyte2 (stack: value ->)
	public static final byte IFNE         = new OpCode("ifne",         0x9A, 3, -1, OpCode.PROP_BRANCH_OFFSET).opcode; // 2: branchbyte1, branchbyte2 (stack: value ->)
	public static final byte IFNONNULL    = new OpCode("ifnonnull",    0xC7, 3, -1, OpCode.PROP_BRANCH_OFFSET).opcode; // 2: branchbyte1, branchbyte2 (stack: value ->)
	public static final byte IFNULL       = new OpCode("ifnull",       0xC6, 3, -1, OpCode.PROP_BRANCH_OFFSET).opcode; // 2: branchbyte1, branchbyte2 (stack: value ->)
	public static final byte IINC         = new OpCode("iinc",         0x84, 3,  0, OpCode.PROP_LOCAL_INDEX | OpCode.PROP_HAS_CUSTOM_EXTRA_BYTES | OpCode.PROP_IS_STANDALONE_VALID).opcode; // 2: index, const (stack: [No change])
	public static final byte ILOAD        = new OpCode("iload",        0x15, 2,  1, OpCode.PROP_LOCAL_INDEX | OpCode.PROP_IS_STANDALONE_VALID).opcode; // 1: index (stack: -> value)
	public static final byte ILOAD_0      = new OpCode("iload_0",      0x1A, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> value)
	public static final byte ILOAD_1      = new OpCode("iload_1",      0x1B, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> value)
	public static final byte ILOAD_2      = new OpCode("iload_2",      0x1C, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> value)
	public static final byte ILOAD_3      = new OpCode("iload_3",      0x1D, 1,  1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> value)
	public static final byte IMPDEP1      = new OpCode("impdep1",      0xFE, 1,  0, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: )
	public static final byte IMPDEP2      = new OpCode("impdep2",      0xFF, 1,  0, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: )
	public static final byte IMUL         = new OpCode("imul",         0x68, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte INEG         = new OpCode("ineg",         0x74, 1,  0, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value -> result)
	public static final byte IOR          = new OpCode("ior",          0x80, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte IREM         = new OpCode("irem",         0x70, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte IRETURN      = new OpCode("ireturn",      0xAC, 1,  0, OpCode.PROP_HAS_VARIABLE_STACK_OFFSET | OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value -> [empty])
	public static final byte ISHL         = new OpCode("ishl",         0x78, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte ISHR         = new OpCode("ishr",         0x7A, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte ISTORE       = new OpCode("istore",       0x36, 2, -1, OpCode.PROP_LOCAL_INDEX | OpCode.PROP_IS_STANDALONE_VALID).opcode; // 1: index (stack: value ->)
	public static final byte ISTORE_0     = new OpCode("istore_0",     0x3B, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value ->)
	public static final byte ISTORE_1     = new OpCode("istore_1",     0x3C, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value ->)
	public static final byte ISTORE_2     = new OpCode("istore_2",     0x3D, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value ->)
	public static final byte ISTORE_3     = new OpCode("istore_3",     0x3E, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value ->)
	public static final byte ISUB         = new OpCode("isub",         0x64, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte IUSHR        = new OpCode("iushr",        0x7C, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte IXOR         = new OpCode("ixor",         0x82, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte JSR          = new OpCode("jsr",          0xA8, 3,  1, OpCode.PROP_BRANCH_OFFSET).opcode; // 2: branchbyte1, branchbyte2 (stack: -> address)
	public static final byte JSR_W        = new OpCode("jsr_w",        0xC9, 5,  1, OpCode.PROP_BRANCH_OFFSET_4).opcode; // 4: branchbyte1, branchbyte2, branchbyte3, branchbyte4 (stack: -> address)
	public static final byte L2D          = new OpCode("l2d",          0x8A, 1,  0, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value -> result)
	public static final byte L2F          = new OpCode("l2f",          0x89, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value -> result)
	public static final byte L2I          = new OpCode("l2i",          0x88, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value -> result)
	public static final byte LADD         = new OpCode("ladd",         0x61, 1, -2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte LALOAD       = new OpCode("laload",       0x2F, 1,  0, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: arrayref, index -> value)
	public static final byte LAND         = new OpCode("land",         0x7F, 1, -2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte LASTORE      = new OpCode("lastore",      0x50, 1, -4, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: arrayref, index, value ->)
	public static final byte LCMP         = new OpCode("lcmp",         0x94, 1, -3, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte LCONST_0     = new OpCode("lconst_0",     0x09, 1,  2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> 0L)
	public static final byte LCONST_1     = new OpCode("lconst_1",     0x0A, 1,  2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> 1L)
	public static final byte LDIV         = new OpCode("ldiv",         0x6D, 1, -2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte LLOAD        = new OpCode("lload",        0x16, 2,  2, OpCode.PROP_LOCAL_INDEX | OpCode.PROP_IS_STANDALONE_VALID).opcode; // 1: index (stack: -> value)
	public static final byte LLOAD_0      = new OpCode("lload_0",      0x1E, 1,  2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> value)
	public static final byte LLOAD_1      = new OpCode("lload_1",      0x1F, 1,  2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> value)
	public static final byte LLOAD_2      = new OpCode("lload_2",      0x20, 1,  2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> value)
	public static final byte LLOAD_3      = new OpCode("lload_3",      0x21, 1,  2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> value)
	public static final byte LMUL         = new OpCode("lmul",         0x69, 1, -2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte LNEG         = new OpCode("lneg",         0x75, 1,  0, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value -> result)
	public static final byte LOR          = new OpCode("lor",          0x81, 1, -2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte LREM         = new OpCode("lrem",         0x71, 1, -2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte LRETURN      = new OpCode("lreturn",      0xAD, 1,  0, OpCode.PROP_HAS_VARIABLE_STACK_OFFSET | OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value -> [empty])
	public static final byte LSHL         = new OpCode("lshl",         0x79, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte LSHR         = new OpCode("lshr",         0x7B, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte LSTORE       = new OpCode("lstore",       0x37, 2, -2, OpCode.PROP_LOCAL_INDEX | OpCode.PROP_IS_STANDALONE_VALID).opcode; // 1: index (stack: value ->)
	public static final byte LSTORE_0     = new OpCode("lstore_0",     0x3F, 1, -2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value ->)
	public static final byte LSTORE_1     = new OpCode("lstore_1",     0x40, 1, -2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value ->)
	public static final byte LSTORE_2     = new OpCode("lstore_2",     0x41, 1, -2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value ->)
	public static final byte LSTORE_3     = new OpCode("lstore_3",     0x42, 1, -2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value ->)
	public static final byte LSUB         = new OpCode("lsub",         0x65, 1, -2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte LUSHR        = new OpCode("lushr",        0x7D, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte LXOR         = new OpCode("lxor",         0x83, 1, -2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value1, value2 -> result)
	public static final byte MONITORENTER = new OpCode("monitorenter", 0xC2, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: objectref ->)
	public static final byte MONITOREXIT  = new OpCode("monitorexit",  0xC3, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: objectref ->)
	public static final byte NEWARRAY     = new OpCode("newarray",     0xBC, 2,  0, OpCode.PROP_HAS_CUSTOM_EXTRA_BYTES | OpCode.PROP_IS_STANDALONE_VALID).opcode; // 1: atype (stack: count -> arrayref)
	public static final byte NOP          = new OpCode("nop",          0x00, 1,  0, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: [No change])
	public static final byte POP          = new OpCode("pop",          0x57, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value ->)
	public static final byte POP2         = new OpCode("pop2",         0x58, 1, -2, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: {value2, value1} ->)
	public static final byte RET          = new OpCode("ret",          0xA9, 2,  0, OpCode.PROP_LOCAL_INDEX | OpCode.PROP_IS_STANDALONE_VALID).opcode; // 1: index (stack: [No change])
	public static final byte RETURN       = new OpCode("return",       0xB1, 1,  0, OpCode.PROP_HAS_VARIABLE_STACK_OFFSET | OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: -> [empty])
	public static final byte SALOAD       = new OpCode("saload",       0x35, 1, -1, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: arrayref, index -> value)
	public static final byte SASTORE      = new OpCode("sastore",      0x56, 1, -3, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: arrayref, index, value ->)
	public static final byte SIPUSH       = new OpCode("sipush",       0x11, 3,  1, OpCode.PROP_HAS_CUSTOM_EXTRA_BYTES | OpCode.PROP_IS_STANDALONE_VALID).opcode; // 2: byte1, byte2 (stack: -> value)
	public static final byte SWAP         = new OpCode("swap",         0x5F, 1,  0, OpCode.PROP_IS_STANDALONE_VALID).opcode; // (stack: value2, value1 -> value1, value2)
	public static final byte WIDE         = new OpCode("wide",         0xC4, 0,  0, OpCode.PROP_HAS_CUSTOM_EXTRA_BYTES | OpCode.PROP_HAS_VARIABLE_LENGTH | OpCode.PROP_HAS_VARIABLE_STACK_OFFSET | OpCode.PROP_IS_STANDALONE_VALID).opcode; // 3/5: opcode, indexbyte1, indexbyte2 (stack: [same as for corresponding instructions])

	private byte[] bytes = new byte[256];
	private int length = 0;
	private int maxStackSize = 0;
	private int stackSize = 0;
	private int maxLocalVariableIndex = 0; // Always include index 0 to support "this" pointer to support non-static methods
	private final Set<Label> labels = new LinkedHashSet<>();

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

	private static class OpCode {
		/** The mask for the bytes following the opcode */
		public static final int PROP_EXTRA_BYTES_MASK = 0x03;
		/** A 1 byte local variable index follows */
		public static final int PROP_LOCAL_INDEX = 0x01;
		/** A 2 byte constant pool index follows */
		public static final int PROP_CONST_POOL_INDEX = 0x02;
		/** A 2 byte branch offset follows */
		public static final int PROP_BRANCH_OFFSET = 0x03;

		/** The opcode has a custom set of bytes that follow */
		public static final int PROP_HAS_CUSTOM_EXTRA_BYTES = 0x04;
		/** The opcode has a variable length */
		public static final int PROP_HAS_VARIABLE_LENGTH = 0x08;
		/** The opcode has a variable stack offset */
		public static final int PROP_HAS_VARIABLE_STACK_OFFSET = 0x10;
		/** The opcode is valid to use as a stand-alone instruction */
		public static final int PROP_IS_STANDALONE_VALID = 0x80;

		/** A 4 byte branch offset follows */
		public static final int PROP_BRANCH_OFFSET_4 = PROP_BRANCH_OFFSET | PROP_HAS_CUSTOM_EXTRA_BYTES;

		public final String mnemonic;
		public final byte opcode;
		public final byte length;
		public final byte stackOffset;
		public final byte properties;

		/**
		 * Creates a new opcode.
		 *
		 * @param mnemonic the string mnemonic for the opcode
		 * @param opcode the byte identifier for the opcode
		 * @param length the length of the opcode
		 * @param stackOffset the stack offset as a result of executing the opcode
		 * @param properties the properties of the opcode
		 */
		public OpCode(final String mnemonic, final int opcode, final int length, final int stackOffset, final int properties) {
			this.mnemonic = mnemonic;
			this.opcode = (byte)opcode;
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

			OPCODES[this.opcode & 0xFF] = this;
		}

		/**
		 * Checks if the opcode has any of the specified properties. The specified properties are bitwise anded with the opcodes properties and compared with zero.
		 *
		 * @param properties the properties to test
		 * @return true if the opcode has any of the specified properties, otherwise false
		 */
		public boolean has(final int properties) {
			return (this.properties & properties) != 0;
		}
	}

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
		if (member instanceof Method) {
			final Method method = (Method)member;
			int offset = double.class.equals(method.getReturnType()) || long.class.equals(method.getReturnType()) ? 2 : 1;

			for (final Class<?> type : method.getParameterTypes()) {
				offset -= double.class.equals(type) || long.class.equals(type) ? 2 : void.class.equals(type) ? 0 : 1;
			}

			return offset;
		} else {
			int offset = 0;

			for (final Class<?> type : ((Constructor<?>)member).getParameterTypes()) {
				offset -= double.class.equals(type) || long.class.equals(type) ? 2 : 1;
			}

			return offset;
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
		final OpCode opcode = OPCODES[instruction & 0xFF];

		if (opcode == null || !opcode.has(OpCode.PROP_BRANCH_OFFSET)) {
			final String mnemonic = (opcode == null ? "0x" + Integer.toHexString(instruction & 0xFF) : opcode.mnemonic);
			throw new IllegalArgumentException("Unexpected bytecode instruction: " + mnemonic + ", expecting a branch instruction");
		}

		label.addReference(this, length, length + 1);

		if (instruction == GOTO_W) {
			return append(GOTO, B0, B0);
		} else if (instruction == JSR_W) {
			maxStackSize = Math.max(maxStackSize, ++stackSize);
			return append(JSR, B0, B0);
		}

		maxStackSize = Math.max(maxStackSize, stackSize += opcode.stackOffset);
		return append(instruction, B0, B0);
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
		int i = 0;

		while (i < code.length) {
			final OpCode opcode = OPCODES[code[i] & 0xFF];

			if (opcode == null) {
				throw new IllegalArgumentException("Invalid bytecode instruction: 0x" + Integer.toHexString(code[i] & 0xFF));
			} else if (!opcode.has(OpCode.PROP_IS_STANDALONE_VALID)) {
				if ((opcode.properties & OpCode.PROP_EXTRA_BYTES_MASK) == OpCode.PROP_BRANCH_OFFSET) {
					throw new IllegalArgumentException("The " + opcode.mnemonic + " instruction must use the addBranch() method");
				} else if (code[i] == GETFIELD || code[i] == GETSTATIC || code[i] == PUTFIELD || code[i] == PUTSTATIC) {
					throw new IllegalArgumentException("The " + opcode.mnemonic + " instruction must use the addFieldAccess() method");
				} else if (code[i] == LOOKUPSWITCH || code[i] == TABLESWITCH) {
					throw new IllegalArgumentException("The " + opcode.mnemonic + " instruction must use the addSwitch() method");
				} else if (code[i] == INVOKEDYNAMIC || code[i] == INVOKEINTERFACE || code[i] == INVOKESPECIAL || code[i] == INVOKESTATIC || code[i] == INVOKEVIRTUAL) {
					throw new IllegalArgumentException("The " + opcode.mnemonic + " instruction must use the addInvoke() method");
				} else if (code[i] == CHECKCAST) {
					throw new IllegalArgumentException("The " + opcode.mnemonic + " instruction must use the addCast() method");
				} else if (code[i] == INSTANCEOF) {
					throw new IllegalArgumentException("The " + opcode.mnemonic + " instruction must use the addInstanceOfCheck() method");
				} else if (code[i] == LDC || code[i] == LDC_W || code[i] == LDC2_W) {
					throw new IllegalArgumentException("The " + opcode.mnemonic + " instruction must use the pushConstant() method");
				} else if (code[i] == ANEWARRAY || code[i] == MULTIANEWARRAY || code[i] == NEW) {
					throw new IllegalArgumentException("The " + opcode.mnemonic + " instruction must use the pushNewObject() method");
				}

				throw new IllegalArgumentException("Invalid bytecode instruction: " + opcode.mnemonic);
			} else if (code[i] == WIDE) {
				if (i + 1 >= code.length) {
					break;
				}

				final OpCode wideOpcode = OPCODES[code[i + 1] & 0xFF];
				final int length;

				if (wideOpcode == null || !wideOpcode.has(OpCode.PROP_LOCAL_INDEX)) {
					final String mnemonic = (wideOpcode == null ? "0x" + Integer.toHexString(code[i + 1] & 0xFF) : wideOpcode.mnemonic);
					throw new IllegalArgumentException("Invalid wide bytecode instruction: " + mnemonic);
				} else if (code[i + 1] == IINC) {
					length = 6;
				} else {
					length = 4;
				}

				if (i + length > code.length) {
					break;
				}

				maxLocalVariableIndex = Math.max(maxLocalVariableIndex, ((code[i + 2] & 0xFF) << 8) + (code[i + 3] & 0xFF));
				i += length;
				maxStackSize = Math.max(maxStackSize, stackSize += wideOpcode.stackOffset);
				continue;
			} else if (opcode.has(OpCode.PROP_LOCAL_INDEX)) {
				maxLocalVariableIndex = Math.max(maxLocalVariableIndex, code[i + 1] & 0xFF);
			}

			i += opcode.length;
			maxStackSize = Math.max(maxStackSize, stackSize += opcode.stackOffset);
		}

		if (i != code.length) {
			throw new IllegalArgumentException("Invalid bytecode (length mismatch)");
		}

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
					if (boolean.class.equals(to) || char.class.equals(to) || int.class.equals(to) || short.class.equals(to) || byte.class.equals(to)) {
						return addCode(L2I).addPrimitiveConversion(int.class, to);
					} else if (float.class.equals(to)) {
						return addCode(L2F);
					} else if (double.class.equals(to)) {
						return addCode(L2D);
					}
				} else if (float.class.equals(from)) {
					if (boolean.class.equals(to) || char.class.equals(to) || int.class.equals(to) || short.class.equals(to) || byte.class.equals(to)) {
						return addCode(F2I).addPrimitiveConversion(int.class, to);
					} else if (long.class.equals(to)) {
						return addCode(F2L);
					} else if (double.class.equals(to)) {
						return addCode(F2D);
					}
				} else if (double.class.equals(from)) {
					if (boolean.class.equals(to) || char.class.equals(to) || int.class.equals(to) || short.class.equals(to) || byte.class.equals(to)) {
						return addCode(D2I).addPrimitiveConversion(int.class, to);
					} else if (long.class.equals(to)) {
						return addCode(D2L);
					} else if (float.class.equals(to)) {
						return addCode(D2F);
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

		throw new IllegalArgumentException("Cannot add a constant of type " + value.getClass().toString());
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
						throw new IllegalArgumentException("Class " + base.getName() + " must have exactly 1 method (contains multiple)");
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
						throw new IllegalArgumentException("Class " + base.getName() + " must have exactly 1 abstract method (contains multiple)");
					}

					method = check;
				}
			}

			if (method == null) {
				for (final Method check : base.getDeclaredMethods()) {
					if ((check.getModifiers() & (Modifier.FINAL | Modifier.NATIVE | Modifier.PRIVATE | Modifier.PROTECTED | Modifier.STATIC)) == 0 && !check.isSynthetic() ) {
						if (method != null) {
							throw new IllegalArgumentException("Class " + base.getName() + " must have exactly 1 abstract method (contains none) or 1 public declared non-final, non-native, non-static method (contains multiple)");
						}

						method = check;
					}
				}
			}

			baseClassInfo = interfaceClassInfo = getConstant(base);
			baseConstructorInfo = getConstant(base.getDeclaredConstructor());
		}

		if (method == null) {
			throw new IllegalArgumentException("Class " + base.getName() + " must have exactly 1 abstract method or 1 public declared non-final, non-native, non-static method (contains none)");
		}

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

		if (length >= 65536 || constantPool.count() >= 65536 || maxStackSize >= 65536 || maxLocalVariableIndex >= 65536) {
			throw new IllegalStateException("Encountered overflow while building method");
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
		final int SUPER = 0x20; // Internal flag
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
							throw new IllegalStateException("Failed to generate code for a branch instruction spanning more than 32767 bytes");
						}

						bytes[location.updateOffset]     = (byte)(branchOffset >>> 8);
						bytes[location.updateOffset + 1] = (byte)(branchOffset);
					}
				}

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
		if (value == (short)value) {
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
				} else {
					return addCode(SIPUSH, (byte)(value >>> 8), (byte)value);
				}
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
		} else if (value == (int)value) {
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
			throw new IllegalArgumentException("Array dimensions to large, expecting at most 255");
		} else if (dimensions.length > 1) {
			for (final int dimension : dimensions) {
				pushConstant(dimension);
			}

			getConstant(Array.newInstance(type, new int[dimensions.length]).getClass()).add(new Location(this, length + 1));
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
			throw new IllegalArgumentException("Unexpected primitive type for new object, expecting non-primitive type");
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
		if (length == 0) {
			return "";
		}

		// Populate all labels and the constant pool
		for (final Label label : labels) {
			label.populate();
		}

		final Object constantPool[] = new Object[this.constantPool.populate().getEntries().size() + 1];

		for (final Entry<Object, ConstantPoolEntry> entry : this.constantPool.getEntries()) {
			constantPool[entry.getValue().index] = entry.getKey();
		}

		final StringBuilder sb = new StringBuilder("[ ");

		// Loop through each instruction (assumes each instruction is already valid and the length is correct)
		for (int i = 0; i < length; ) {
			final OpCode opcode = OPCODES[bytes[i] & 0xFF];
			int constantPoolIndex = 0;
			String postConstant = "";

			assert opcode != null : "Invalid opcode detected: 0x" + Integer.toHexString(bytes[i] & 0xFF);
			sb.append(i).append(": ");

			if (opcode.has(OpCode.PROP_HAS_CUSTOM_EXTRA_BYTES | OpCode.PROP_HAS_VARIABLE_LENGTH)) {
				sb.append(opcode.mnemonic);

				if (bytes[i] == INVOKEINTERFACE || bytes[i] == INVOKEDYNAMIC) {
					constantPoolIndex = ((bytes[i + 1] & 0xFF) << 8) + (bytes[i + 2] & 0xFF);
					i += opcode.length;
				} else if (bytes[i] == LOOKUPSWITCH) {
					final int j = (i + 3) & ~3;
					final int npairs = (bytes[j + 4] << 24) + ((bytes[j + 5] & 0xFF) << 16) + ((bytes[j + 6] & 0xFF) << 8) + (bytes[j + 7] & 0xFF);

					sb.append(' ').append(i + (bytes[j] << 24) + ((bytes[j + 1] & 0xFF) << 16) + ((bytes[j + 2] & 0xFF) << 8) + (bytes[j + 3] & 0xFF));

					for (int k = 1; k <= npairs; k++) {
						sb.append(", ").append((bytes[j + k * 8] << 24) + ((bytes[j + k * 8 + 1] & 0xFF) << 16) + ((bytes[j + k * 8 + 2] & 0xFF) << 8) + (bytes[j + k * 8 + 3] & 0xFF)).append(" -> ").append(i + (bytes[j + k * 8 + 4] << 24) + ((bytes[j + k * 8 + 5] & 0xFF) << 16) + ((bytes[j + k * 8 + 6] & 0xFF) << 8) + (bytes[j + k * 8 + 7] & 0xFF));
					}

					sb.append("; ");
					i = j + 8 + npairs * 8;
				} else if (bytes[i] == MULTIANEWARRAY) {
					constantPoolIndex = ((bytes[i + 1] & 0xFF) << 8) + (bytes[i + 2] & 0xFF);
					postConstant = ", " + (bytes[i + 3] & 0xFF);
					i += 4;
				} else if (bytes[i] == TABLESWITCH) { // Not currently implemented
					final int j = (i + 3) & ~3;
					final int start = (bytes[j + 4] << 24) + ((bytes[j + 5] & 0xFF) << 16) + ((bytes[j + 6]  & 0xFF) << 8) + (bytes[j + 7]  & 0xFF);
					final int end   = (bytes[j + 8] << 24) + ((bytes[j + 9] & 0xFF) << 16) + ((bytes[j + 10] & 0xFF) << 8) + (bytes[j + 11] & 0xFF);

					sb.append(' ').append(i + (bytes[j] << 24) + ((bytes[j + 1] & 0xFF) << 16) + ((bytes[j + 2] & 0xFF) << 8) + (bytes[j + 3] & 0xFF));

					for (int k = 0; k <= end - start; k++) {
						sb.append(", ").append(start + k).append(" -> ").append(i + (bytes[j + k * 4 + 12] << 24) + ((bytes[j + k * 4 + 13] & 0xFF) << 16) + ((bytes[j + k * 4 + 14] & 0xFF) << 8) + (bytes[j + k * 4 + 15] & 0xFF));
					}

					sb.append("; ");
					i = j + 12 + (end - start + 1) * 4;
				} else if (bytes[i] == BIPUSH || bytes[i] == NEWARRAY) {
					sb.append(' ').append(bytes[i + 1]).append("; ");
					i += 2;
				} else if (bytes[i] == GOTO_W || bytes[i] == JSR_W) { // Not currently implemented
					sb.append(' ').append((bytes[i + 1] << 24) + ((bytes[i + 2] & 0xFF) << 16) + ((bytes[i + 3] & 0xFF) << 8) + (bytes[i + 4] & 0xFF)).append("; ");
					i += 5;
				} else if (bytes[i] == IINC) {
					sb.append(' ').append(bytes[i + 1] & 0xFF).append(", ").append(bytes[i + 2]).append("; ");
					i += 3;
				} else if (bytes[i] == LDC) { // Not currently implemented
					constantPoolIndex = bytes[i + 1] & 0xFF;
					i += 2;
				} else if (bytes[i] == SIPUSH) {
					sb.append(' ').append((short)(bytes[i + 1] << 8) + (bytes[i + 2] & 0xFF)).append("; ");
					i += 3;
				} else if (bytes[i] == WIDE) {
					final OpCode wideOpcode = OPCODES[bytes[i + 1] & 0xFF];

					if (bytes[i + 1] == IINC) {
						sb.append(' ').append(wideOpcode.mnemonic).append(' ').append(((bytes[i + 2] & 0xFF) << 8) + (bytes[i + 3] & 0xFF)).append(", ").append((short)(bytes[i + 4] << 8) + (bytes[i + 5] & 0xFF)).append("; ");
						i += 6;
					} else {
						assert wideOpcode.has(OpCode.PROP_LOCAL_INDEX) : "Invalid wide opcode detected: " + wideOpcode == null ? "0x" + Integer.toHexString(bytes[i] & 0xFF) : wideOpcode.mnemonic;
						sb.append(' ').append(wideOpcode.mnemonic).append(' ').append(((bytes[i + 2] & 0xFF) << 8) + (bytes[i + 3] & 0xFF)).append("; ");
						i += 4;
					}
				}
			} else {
				if ((opcode.properties & OpCode.PROP_EXTRA_BYTES_MASK) == OpCode.PROP_LOCAL_INDEX) { // 2-byte opcode, index
					sb.append(opcode.mnemonic).append(' ').append(bytes[i + 1] & 0xFF).append("; ");
				} else if ((opcode.properties & OpCode.PROP_EXTRA_BYTES_MASK) == OpCode.PROP_BRANCH_OFFSET) { // 3-byte opcode, branch offset
					sb.append(opcode.mnemonic).append(' ').append(i + ((bytes[i + 1] & 0xFF) << 8) + (bytes[i + 2] & 0xFF)).append("; ");
				} else if ((opcode.properties & OpCode.PROP_EXTRA_BYTES_MASK) == OpCode.PROP_CONST_POOL_INDEX) { // 3-byte opcode, constant pool index
					sb.append(opcode.mnemonic);
					constantPoolIndex = ((bytes[i + 1] & 0xFF) << 8) + (bytes[i + 2] & 0xFF);
				} else { // 1-byte opcode
					sb.append(opcode.mnemonic).append("; ");
				}

				i += opcode.length;
			}

			// If using a constant pool entry, then print it out
			if (constantPoolIndex > 0) {
				final Object constant = constantPool[constantPoolIndex];

				if (constant instanceof String) {
					sb.append(" \"").append(constant).append(postConstant).append("\"; ");
				} else if (constant instanceof Member) {
					if (constant instanceof Method) {
						String separator = "";
						sb.append(' ').append(((Method)constant).getDeclaringClass().getName()).append('.').append(((Method)constant).getName()).append('(');

						for (final Class<?> parameterType : ((Method)constant).getParameterTypes()) {
							sb.append(separator).append(parameterType.getName());
							separator = ", ";
						}

						sb.append(')').append(postConstant).append("; ");
					} else {
						sb.append(' ').append(((Member)constant).getDeclaringClass().getName()).append('.').append(((Member)constant).getName()).append(postConstant).append("; ");
					}
				} else if (constant instanceof Class) {
					sb.append(' ').append(((Class<?>)constant).getName()).append(postConstant).append("; ");
				} else {
					sb.append(' ').append(constant).append(postConstant).append("; ");
				}
			}
		}

		sb.setLength(sb.length() - 2);
		return sb.append(" ]").toString();
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
