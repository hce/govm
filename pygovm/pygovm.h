#ifndef __PYGOVM_H
#define __PYGOVM_H

enum {
    INSTR_SYSCALL = 0,
    INSTR_LI = 1,
    INSTR_JMP = 2,
    INSTR_JZ = 3,
    INSTR_LB = 4,
    INSTR_LW = 5,
    INSTR_SB = 6,
    INSTR_SW = 7,
    INSTR_ADD = 8,
    INSTR_SALLOC = 9,
    INSTR_DIV = 10,
    INSTR_NOR = 11,
    INSTR_POP = 12,
    INSTR_DUP = 13,
    INSTR_ROT = 14,
    INSTR_ROT3 = 15,
    INSTR_MOVA = 16,
    INSTR_MOVB = 17,
    INSTR_MOVC = 18,
    INSTR_MOVD = 19,
    INSTR_MOVE = 20,
    INSTR_MOVF = 21,
    INSTR_AMOV = 22,
    INSTR_BMOV = 23,
    INSTR_CMOV = 24,
    INSTR_DMOV = 25,
    INSTR_EMOV = 26,
    INSTR_FMOV = 27,
    INSTR_CALL = 28,
    INSTR_LWS = 29,
    INSTR_SWS = 30,
    INSTR_SUB = 31,
    INSTR_NOT = 32,
    INSTR_EQU = 33,
    INSTR_LOE = 34,
    INSTR_GOE = 35,
    INSTR_LT = 36,
    INSTR_GT = 37,
    INSTR_AND = 38,
    INSTR_OR = 39,
    INSTR_SHL = 40,
    INSTR_SHR = 41,
    INSTR_MUL = 42,
    INSTR_NOP = 43,
} e_INSTR;

enum {
    SYSCALL_HLT = 0,
    SYSCALL_PUTC = 1,
    SYSCALL_GETC = 2,
    SYSCALL_INFO = 3,
    SYSCALL_GETS = 4,
    SYSCALL_OPEN = 5,
    SYSCALL_CLOSE = 6,
    SYSCALL_FGETC = 7,
    SYSCALL_FPUTC = 8,
} e_SYSCALL;

enum {
    EOPSYSOK = 0,
    EOPSYSINVALID = 1,
    EOPSYSTERMINATE = 2,
    EOPSYSSTACKUFLOW = 3,
    EOPSYSSTACKOFLOW = 4,
    EPYEXCEPTION = 5,
} e_EOPSYS;


enum {
    ERR_READ = 0,
    ERR_SIZE = 1,
    ERR_PEOF = 2,
} e_ERR;

struct PYGOVM_callinfo {
    PyObject* cb_putc;
    PyObject* cb_getc;
    int l_gets;
    const char* s_gets;
    int l_data;
    const char* s_data;
};

#ifndef TRUE
#define TRUE 1
#define FALSE 0
#endif

#endif
