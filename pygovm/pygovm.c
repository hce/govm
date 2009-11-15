#include <Python.h>

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>

#include "pygovm.h"

#define STACK_SIZE 8192
#define STACK_DUMP_VALUES 8

typedef uint32_t dword;
typedef int16_t word;
typedef uint8_t byte;
typedef uint16_t uword;
typedef int boolean;

static byte *cs = NULL;
static byte *ds = NULL;
static uword cs_size;
static uword ds_size;
static word *ss;
static word sp = 0;
static word pc;
static boolean did_jump;
static byte cur_instr_byte;
static boolean bigendian;

// Registers
static word aft;
static word bur;
static word con;
static word dis;

static word bp = 0;

// #define _DEBUG
// #define _DEBUGSTACK
// #define _SSTOREDEBUG
// #define _DEBUG_CALL

static word* registers[] = {&aft, &bur, &con, &dis, &sp, &bp};


static void do_stack_dump(void)
{
	int i;
	for (i = 0; i < sp; i++) {
		if ((i % STACK_DUMP_VALUES) == 0) {
			fprintf(stderr, "%02x  ", i);
		}

		if (bigendian) {
			fprintf(stderr, "%02x%02x", (uint16_t) ss[i] >> 8, (uint16_t) ss[i] & 0xFF);
		} else {
			fprintf(stderr, "%02x%02x", ss[i] & 0xFF, ss[i] >> 8);
		}

		if ((i % STACK_DUMP_VALUES) == (STACK_DUMP_VALUES - 1)) {
			fprintf(stderr, "\n");
		} else {
			fprintf(stderr, " ");
		}
	}
	fprintf(stderr, "\n");
}

static int bread(void* buf, int a, int b, const char** src)
{
    int len;

    len = a * b;
    memcpy(buf, *src, len);
    *src += len;
    return len;
}

static boolean push(word w)
{
	if (sp >= STACK_SIZE) {
		return FALSE;
	}
	ss[sp++] = w;
#ifdef _DEBUGSTACK
	do_stack_dump();
#endif
	return TRUE;
}

static boolean pop(word *w)
{
	if (sp <= 0) {
		return FALSE;
	}
	*w = ss[--sp];
#ifdef _DEBUGSTACK
	do_stack_dump();
#endif
	return TRUE;
}

static boolean checkhdr(const char **f)
{
	byte buf[4];
	bread(buf, 1, 4, f);
	return memcmp(buf, "GOVM", 4) == 0;
}

static boolean readword(const char **f, uword *w)
{
	byte buf[2];
	if (bread(buf, 1, 2, f) != 2) {
		return FALSE;
	}
	if (bigendian) {
		*w = ((*buf) << 8) + 1[buf];
	} else {
		*w = *buf + (1[buf] << 8);
	}
	return TRUE;
}

static int init_data(const char** f, uword i_size)
{
	if (bread(ds, 1, i_size, f) != i_size) {
		return -ERR_PEOF;
	}

	return 0;
}

static int _fetch_instruction(void)
{
	boolean even;
	int byte;

	even = ((pc & 0x01) == 0);
	byte = pc >> 1;
	cur_instr_byte = byte[cs];
	if (!bigendian) even = !even;
	if (even) return cur_instr_byte >> 4;
	else return cur_instr_byte & 0x0F;
}

static int fetch_instruction(void)
{
    int foo;
    foo = _fetch_instruction();
    if (pc < 0) {
        fprintf(stderr, "FETCH_INSTRUCTION: %d at %d\n", foo, pc);
    }
    return foo;
}

static boolean op_load_instant(void)
{
	int val = 0;
	int i;

	if ((pc >> 1) >= cs_size) {
		fprintf(stderr, "govm: op_load_instant: ran out of code...\n");
		return FALSE;
	}

	if (bigendian) {
		for (i = 12; i >= 0; i-=4) {
			pc++;
			val |= fetch_instruction() << i;
		}
	} else {
		for (i = 0; i < 16; i+=4) {
			pc++;
			val |= fetch_instruction() << i;
		}
	}

#ifdef _DEBUG
	fprintf(stderr, "govm: DEBUG: LI: %d\n", val);
#endif

	if (!push(val)) {
		fprintf(stderr, "govm: stack overflow!");
		return FALSE;
	}

	return TRUE;
}

static int syscall_gets(struct PYGOVM_callinfo* ci)
{
    word lsp;
    int len;

    if(!pop(&lsp)) return -1;
    if(!pop(&lsp)) return -1;
    len = ci->l_gets;
    if (len > 90) return -1;
    if ((lsp - (len / 2)) > lsp) return -1;
    if ((lsp - (len / 2)) < 0) return -1;
    if (lsp >= STACK_SIZE) return -1;
    char* addr = (char*)&ss[lsp];
    memcpy(addr - len, ci->s_gets, len);
    return 0;
}


static int py_putc(struct PYGOVM_callinfo *ci, unsigned char c)
{
    PyObject* args;
    PyObject* res;
    unsigned char s[2] = " ";

    *s = c;
    args = Py_BuildValue("(s#)", s, 1);
    res = PyObject_CallObject(ci->cb_putc, args);
    Py_DECREF(args);
    if (res == NULL) return -1;
    Py_DECREF(res);
    return 0;
}

static int py_getc(struct PYGOVM_callinfo *ci)
{
    PyObject* args;
    PyObject* res;
    Py_ssize_t len;
    char* s;
    char c;

    args = Py_BuildValue("()");
    res = PyObject_CallObject(ci->cb_getc, args);
    Py_DECREF(args);
    if (res == NULL) {
        PyErr_SetString(PyExc_TypeError, "No return value :-(");
        return -1;
    }
    if (!PyString_Check(res)) {
        PyErr_SetString(PyExc_TypeError, "must return a string");
        return -1;
    }
    PyString_AsStringAndSize(res, &s, &len);
    if (len != 1) {
        PyErr_SetString(PyExc_TypeError, "string must be one byte long");
        return -1;
    }
    c = *s;
    Py_DECREF(res);
    return c;
}

static int
syscall_open(void)
{
    int fh;
    word _sptr;
    uword sptr;
    word mode;
    uword i;
    word _dummy;

    if (!pop(&_dummy)) {
        return -EOPSYSSTACKUFLOW;
    }
    if (!pop(&mode)) {
		return -EOPSYSSTACKUFLOW;
    }
    if (!pop(&_sptr)) {
		return -EOPSYSSTACKUFLOW;
    }
    sptr = (uint16_t) _sptr;
    if (sptr > ds_size) {
        PyErr_SetString(PyExc_TypeError, "Illegal pointer");
        return -EPYEXCEPTION;
    }
    for (i = sptr; (i < ds_size) && ds[i]; i++);
    if (i >= ds_size) {
        PyErr_SetString(PyExc_TypeError, "No terminating zero");
        return -EPYEXCEPTION;
    }
    fh = open(&((char*)ds)[sptr], mode);
    if (fh >= 65536) {
        PyErr_SetString(PyExc_TypeError, "open returned a too large file descriptor");
        return -EPYEXCEPTION;
    }
    push(fh);
    return EOPSYSOK;
}

static int
syscall_fgetc(void)
{
    word fh;
    char _c;
    word _dummy;

    if (!pop(&_dummy)) {
        return -EOPSYSSTACKUFLOW;
    }
    if (!pop(&fh)) {
		return -EOPSYSSTACKUFLOW;
    }
    if (read(fh, &_c, 1) != 1) {
        push(-1);
        return EOPSYSOK;
    }
    push(_c);
    return EOPSYSOK;
}

static int
syscall_fputc(void)
{
    word fh;
    word c;
    char _c;
    word _dummy;

    if (!pop(&_dummy)) {
        return -EOPSYSSTACKUFLOW;
    }
    if (!pop(&c)) {
		return -EOPSYSSTACKUFLOW;
    }
    if (!pop(&fh)) {
		return -EOPSYSSTACKUFLOW;
    }
    _c = (char) c;
    if (write(fh, &_c, 1) != 1) {
        push(-1);
        return EOPSYSOK;
    }
    push(0);
    return EOPSYSOK;
}

static int
syscall_close(void)
{
    word fh;
    word _dummy;

    if (!pop(&_dummy)) {
        return -EOPSYSSTACKUFLOW;
    }
    if (!pop(&fh)) {
		return -EOPSYSSTACKUFLOW;
    }
    close(fh);
    return EOPSYSOK;
}

static int op_syscall(struct PYGOVM_callinfo* ci, byte syscall)
{
	word s1;
	byte b1;
    int  i1;

	switch (syscall) {
	case SYSCALL_HLT:
		return -EOPSYSTERMINATE;

	case SYSCALL_PUTC:
		if (sp == 0) {
			return -EOPSYSSTACKUFLOW;
		}
		s1 = ss[sp - 2];
		b1 = s1 & 0xFF;
        if(py_putc(ci, b1)) return -EPYEXCEPTION;
		return EOPSYSOK;

	case SYSCALL_GETC:
		i1 = py_getc(ci);
        if (i1 == -1) return -EPYEXCEPTION;
		if (!push((byte) i1)) {
			return -EOPSYSSTACKOFLOW;
		}
		return EOPSYSOK;

    case SYSCALL_OPEN:
        return syscall_open();

    case SYSCALL_FGETC:
        return syscall_fgetc();

    case SYSCALL_CLOSE:
        return syscall_close();

    case SYSCALL_FPUTC:
        return syscall_fputc();

    case SYSCALL_GETS:
        if (syscall_gets(ci)) {
            return -EOPSYSSTACKUFLOW;
        }
        return EOPSYSOK;

	case SYSCALL_INFO:
		fprintf(stderr, "-----------------------------------------------------------------\n");
		fprintf(stderr, "govm: info syscall issued\n");
		fprintf(stderr, "ds_size: %d; cs_size: %d\n", ds_size, cs_size);
		do_stack_dump();
		fprintf(stderr, "-----------------------------------------------------------------\n");
		return EOPSYSOK;
	}
	return -EOPSYSINVALID;
}

static int salloc(word bytes)
{
    word i;

    for (i = 0; i <= bytes; i++) {
        if (!push(0)) {
            return -1;
        }
    }
    return 0;
}

/* Safe abs function that does not depend on math library */
static word
safe_abs(word val)
{
    if (val >= 0) return val;
    return -val;
}

/* Do the real work */
static int vm_run(struct PYGOVM_callinfo* ci)
{
	int instr;
	int retval = 0;
    int max_instrs = 1 << 20;

	word s0;
	word s1;

	word tmp;
    word dest;
	int itmp;
	int cs_nsize;

	did_jump = TRUE;
	cs_nsize = cs_size << 1;
	while (pc < cs_nsize) {
        if (--max_instrs <= 0) return -1;
		instr = fetch_instruction();
        if (instr & 0x08) {
            ++pc;
            instr &= ~0x08;
            instr |= (((int) fetch_instruction()) << 3);
        }
		did_jump = FALSE;
#ifdef _DEBUG
		fprintf(stderr, "govm: DEBUG: instr_fetch: %01X (%d)\n", instr, instr);
#endif
        if (pc < 0) {
    		fprintf(stderr, "govm: DEBUG: instr_fetch: %01X (%d)\n", instr, instr);
        }
		switch (instr) {
		case INSTR_LI:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: LI\n");
#endif
			if (!op_load_instant()) {
				goto error_bail_out;
			}
			break;

		case INSTR_LB:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: LB\n");
#endif
			if (sp == 0) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			s0 = ss[sp - 1];
			if (s0 >= (ds_size + ci->l_data)) {
				fprintf(stderr, "govm: LB access violation\n");
				goto error_bail_out;
			}
			push(ds[s0]);

			break;

		case INSTR_LW:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: LW\n");
#endif
			if (sp == 0) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			// replace in-memory rather than popping and pushing
			// for performance
			s0 = ss[sp - 1];
			if (s0 >= (ds_size - 1)) {
				fprintf(stderr, "govm: LW access violation\n");
				goto error_bail_out;
			}
			if (bigendian) {
				s0 = ds[s0 + 1] + (ds[s0] << 8);
			} else {
				s0 = ds[s0] + (ds[s0 + 1] << 8);
			}
			push(s0);
			break;

		case INSTR_LWS:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: LWS\n");
#endif
			if (sp == 0) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			// replace in-memory rather than popping and pushing
			// for performance
			s0 = ss[sp - 1];
			if (s0 >= (STACK_SIZE - 1)) {
				fprintf(stderr, "govm: LWS access violation\n");
				goto error_bail_out;
			}
#ifdef _SSTOREDEBUG
			fprintf(stderr, "govm: read %d (%d)\n", s0, ss[s0]);
#endif
			s0 = ss[s0];
			push(s0);
			break;

		case INSTR_SB:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: SB\n");
#endif
			if (sp <= 1) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			s0 = ss[sp - 1];
			s1 = ss[sp - 2];
			if (s1 >= ds_size) {
				fprintf(stderr, "govm: SB access violation\n");
				goto error_bail_out;
			}
			ds[s1] = s0 & 0xFF;
			break;

		case INSTR_SW:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: SW\n");
#endif
			if (sp <= 1) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			s0 = ss[sp - 1];
			s1 = ss[sp - 2];
			if (s1 >= (ds_size - 1)) {
				fprintf(stderr, "govm: SW access violation\n");
				goto error_bail_out;
			}
			if (bigendian) {
				ds[s1] = s0 >> 8;
				ds[s1 + 1] = s0 & 0xFF;
			} else {
				ds[s1 + 1] = s0 >> 8;
				ds[s1] = s0 & 0xFF;
			}
			break;

		case INSTR_SWS:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: SW\n");
#endif
			if (sp <= 1) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			s0 = ss[sp - 1];
			s1 = ss[sp - 2];
			if (s1 >= (STACK_SIZE - 1)) {
				fprintf(stderr, "govm: SWS access violation\n");
				goto error_bail_out;
			}
#ifdef _SSTOREDEBUG
			fprintf(stderr, "govm: %d = %d\n", s1, s0);
#endif
			ss[s1] = s0;
			break;


		case INSTR_POP:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: POP\n");
#endif
			if (!pop(&tmp)) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			break;

		case INSTR_DUP:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: DUP\n");
#endif
			if (sp == 0) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			push(ss[sp - 1]);
			break;

		case INSTR_ROT:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: ROT\n");
#endif
			if (sp <= 1) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			tmp = ss[sp - 1];
			ss[sp - 1] = ss[sp - 2];
			ss[sp - 2] = tmp;
			break;

		case INSTR_ROT3:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: ROT3\n");
#endif
			if (sp <= 2) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			tmp = ss[sp - 1];
			ss[sp - 1] = ss[sp - 3];
			ss[sp - 3] = ss[sp - 2];
			ss[sp - 2] = tmp;
			break;

		case INSTR_XOR:
#ifdef _debug
			fprintf(stderr, "govm: debug: xor\n");
#endif
			if (sp <= 1) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			sp -= 2;
			push(ss[sp] ^ ss[sp + 1]);
			break;

		case INSTR_ADD:
#ifdef _debug
			fprintf(stderr, "govm: debug: add\n");
#endif
			if (sp <= 1) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			sp -= 2;
			push(ss[sp] + ss[sp + 1]);
			break;

		case INSTR_SUB:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: ADD\n");
#endif
			if (sp <= 1) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			sp -= 2;
			push(ss[sp] - ss[sp + 1]);
			break;


		case INSTR_SALLOC:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: SALLOC\n");
#endif
			if (sp == 0) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
            if (salloc(ss[sp - 1] - 1)) {
    			goto error_bail_out;
            }
			break;

		case INSTR_DIV:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: DIV\n");
#endif
			if (sp <= 1) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			sp -= 2;
			push(ss[sp] / ss[sp + 1]);
			break;

		case INSTR_MUL:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: MUL\n");
#endif
			if (sp <= 1) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			sp -= 2;
			push(ss[sp] * ss[sp + 1]);
			break;

		case INSTR_AND:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: AND\n");
#endif
			if (sp <= 1) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			sp -= 2;
			push(ss[sp] & ss[sp + 1]);
			break;

		case INSTR_OR:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: OR\n");
#endif
			if (sp <= 1) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			sp -= 2;
			push(ss[sp] | ss[sp + 1]);
			break;

		case INSTR_SHL:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: SHL\n");
#endif
			if (sp <= 1) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			sp -= 2;
			push(ss[sp] << ss[sp + 1]);
			break;

		case INSTR_SHR:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: SHR\n");
#endif
			if (sp <= 1) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			sp -= 2;
			push(ss[sp] >> ss[sp + 1]);
			break;


		case INSTR_NOR:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: NOR\n");
#endif
			if (sp <= 1) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			sp -= 1;
			ss[sp] = ~(ss[sp] | ss[sp + 1]);
			break;

		case INSTR_EQU:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: EQU (%d == %d)\n", ss[sp - 1], ss[sp - 2]);
#endif
			if (sp <= 1) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			sp -= 1;
			ss[sp - 1] = ss[sp] == ss[sp - 1];
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: EQU RESULT: %d\n", ss[sp]);
#endif
			break;

		case INSTR_LOE:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: LOE\n");
#endif
			if (sp <= 1) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			sp -= 1;
			ss[sp - 1] = ss[sp - 1] <= ss[sp];
			break;

		case INSTR_GOE:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: GOE\n");
#endif
			if (sp <= 1) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			sp -= 1;
			ss[sp - 1] = ss[sp - 1] >= ss[sp];
			break;

		case INSTR_LT:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: LT\n");
#endif
			if (sp <= 1) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			sp -= 1;
			ss[sp - 1] = ss[sp - 1] < ss[sp];
			break;

		case INSTR_GT:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: GT\n");
#endif
			if (sp <= 1) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			sp -= 1;
			ss[sp - 1] = ss[sp - 1] > ss[sp];
			break;

        case INSTR_NOT:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: NOT\n");
#endif
			if (sp <= 1) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			ss[sp - 1] = !ss[sp - 1];
			break;

        case INSTR_NOP:
            break;

		case INSTR_JMP:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: JMP\n");
#endif
			if (!pop(&s0)) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
            dest = safe_abs(s0);
			if ((dest >> 1) >= cs_size) {
				fprintf(stderr, "govm: JMP access violation\n");
				fprintf(stderr, "govm: want to jump to %d which exceeds \n"
						"     cs_size [%d]!\n", dest, cs_size << 1);
				goto error_bail_out;
			}
#ifdef _DEBUG_CALL
            fprintf(stderr, "govm: JMP: %d\n", dest);
#endif
			pc = dest;
			did_jump = TRUE;
			break;

		case INSTR_CALL:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: JMP\n");
#endif
			if (!pop(&s0)) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			if ((s0 >> 1) >= cs_size) {
				fprintf(stderr, "govm: CALL access violation\n");
				fprintf(stderr, "govm: want to jump to %d which exceeds \n"
						"     cs_size [%d]!\n", s0, cs_size << 1);
				goto error_bail_out;
			}
#ifdef _DEBUG_CALL
            fprintf(stderr, "govm: CALL to %d\n", s0);
#endif
            push(pc + 1);
			pc = s0;
			did_jump = TRUE;
			break;

		case INSTR_JZ:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: JZ\n");
#endif
			if (sp <= 1) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			s0 = ss[sp - 1];
			s1 = ss[sp - 2];
#ifdef _DEBUG
			fprintf(stderr, "govm: s0 == %d\n", s0);
			fprintf(stderr, "govm: s1 == %d\n", s1);
#endif
			sp -= 1;
			if (s1) {
				break;
			}
			if ((s0 >> 1) >= cs_size) {
				fprintf(stderr, "govm: JZ access violation\n");
				goto error_bail_out;
			}
			pc = s0;
			did_jump = TRUE;
			break;

		case INSTR_SYSCALL:
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: SYSCALL\n");
#endif
			if (sp == 0) {
				fprintf(stderr, "govm: stack underflow\n");
				goto error_bail_out;
			}
			s0 = ss[sp - 1];
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: syscall: %d\n", s0 & 0xFF);
#endif
			itmp = op_syscall(ci, s0 & 0xFF);
#ifdef _DEBUG
			fprintf(stderr, "govm: DEBUG: syscall returned : %d\n", itmp);
#endif
			if (itmp < 0) {
				switch (-itmp) {
				case EOPSYSTERMINATE:
#ifdef _DEBUG
					fprintf(stderr, "govm: halted\n");
#endif
					goto bail_out;
                case EPYEXCEPTION:
                    retval = -2;
                    goto bail_out;

				case EOPSYSINVALID:
					fprintf(stderr, "govm: invalid syscall\n");
					goto error_bail_out;

				case EOPSYSSTACKUFLOW:
					fprintf(stderr, "govm: syscall: stack underflow\n");
					goto error_bail_out;

				case EOPSYSSTACKOFLOW:
					fprintf(stderr, "govm: syscall: stack overflow\n");
					goto error_bail_out;
				}
				fprintf(stderr, "govm: unknown error %d\n", tmp);
				goto error_bail_out;
			}
			break;

		case INSTR_MOVA...INSTR_MOVF:
			pop(registers[instr - INSTR_MOVA]);
			break;
		case INSTR_AMOV...INSTR_FMOV:
			push(*registers[instr - INSTR_AMOV]);
			break;
		}
		if (!did_jump) {
			pc++;
		}
#ifdef _DEBUG
		fprintf(stderr, "govm: DEBUG: %d values on the stack\n", sp);
#endif
	}

error_bail_out:
	fprintf(stderr, "govm: ERROR; ABORTING. Stack dump follows...\n");
	do_stack_dump();
	retval = 1;
bail_out:
	return retval;
}

static int exec(const char *program, struct PYGOVM_callinfo* ci)
{
	char buf[4];
	int retval;
	uword i_size;
    char* progmem;

    progmem = malloc(1 << 20);
    if (!progmem) {
        fprintf(stderr, "govm: out of memory\n");
        return 1;
    }

    if (!checkhdr(&program)) {
        fprintf(stderr, "govm: illegal header\n");
        return 1;
    }

	bread(buf, 1, 1, &program);
	switch (buf[0]) {
		case 0x10:
			bigendian = FALSE;
			break;

		case 0x11:
			bigendian = TRUE;
			break;

		default:
			fprintf(stderr, "govm: illegal format\n");
			return 1;
	}

#ifdef _DEBUG
	fprintf(stderr, "govm: DEBUG: %s endian\n", bigendian ? "big":"little");
#endif

	// get csize
	if (!readword(&program, &cs_size)) {
		fprintf(stderr, "govm: read error\n");
		return 1;
	}

	// get dsize
	if (!readword(&program, &ds_size)) {
		fprintf(stderr, "govm: read error\n");
		return 1;
	}

	// read isize
	if (!readword(&program, &i_size)) {
		fprintf(stderr, "govm: read error\n");
		return 1;
	}
	if (i_size > ds_size) {
		fprintf(stderr, "govm: error: i_size > ds_size!\n");
		return 1;
	}

	// skip bsize
	if (bread(buf, 1, 2, &program) != 2) {
		fprintf(stderr, "govm: premature EOF\n");
		return 1;
	}

	// get instruction pointer
	if (!readword(&program, (uword*)&pc)) {
		fprintf(stderr, "govm: read error\n");
		return 1;
	}

#ifdef _DEBUG
    fprintf(stderr, "govm: CS == %d bytes\n", cs_size);
    fprintf(stderr, "govm: DS == %d bytes (%d initialized)\n", ds_size, i_size);
    fprintf(stderr, "govm: IP == %d\n", pc);
#endif

	if ((pc >> 1) >= cs_size) {
		fprintf(stderr, "govm: IP > CS\n");
		return 1;
	}


    ss = (word*) progmem;
	cs = (byte *) progmem + (STACK_SIZE << 1);
	ds = (byte*) progmem + cs_size + (STACK_SIZE << 1);

    memcpy(ds + ds_size, ci->s_data, ci->l_data);

	if (bread(cs, 1, cs_size, &program) != cs_size) {
        PyErr_SetString(PyExc_TypeError, "GoVM file corrupted");
		free(progmem);
		return 1;
	}

	if (i_size > 0) {
		retval = init_data(&program, i_size);
		if (retval) {
            PyErr_SetString(PyExc_TypeError, "GoVM file corrupted");
		    free(progmem);
			return 1;
		}
	}

	retval = vm_run(ci);

	free(progmem);
	return retval;
}

static PyObject *
govm_run(PyObject *self, PyObject *args)
{
    const char *program;
    struct PYGOVM_callinfo ci;
    int len;
    int sts;

    if (!PyArg_ParseTuple(args, "s#s#s#OO", &program, &len, &ci.s_gets, &ci.l_gets, &ci.s_data, &ci.l_data, &ci.cb_putc, &ci.cb_getc)) {
        return NULL;
    }
    if (!PyCallable_Check(ci.cb_putc)) {
        PyErr_SetString(PyExc_TypeError, "parameter #3 must be callable");
        return NULL;
    }
    if (!PyCallable_Check(ci.cb_getc)) {
        PyErr_SetString(PyExc_TypeError, "parameter #4 must be callable");
        return NULL;
    }
    sts = exec(program, &ci);
    if (sts < 0) return NULL; /* python exception */
    return Py_BuildValue("i", sts);
}


static PyMethodDef GOVMMethods[] = {
    {"run",  govm_run, METH_VARARGS,
     "Run a program."},
    {NULL, NULL, 0, NULL}        /* Sentinel */
};

PyMODINIT_FUNC
initgovm(void)
{
    (void) Py_InitModule("govm", GOVMMethods);
}

