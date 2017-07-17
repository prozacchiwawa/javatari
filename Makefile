examples/%.class: %.fr
	./fregec $<

all: examples/IExpr.class examples/ITest.class

Expr.fr: examples/Util.class examples/FFI.class
	touch $@

Instr.fr: examples/Util.class examples/Expr.class
	touch $@

IExpr.fr: examples/Util.class examples/FFI.class examples/Expr.class examples/Instr.class
	touch $@

ITest.fr: examples/Util.class examples/FFI.class
	touch $@

run: all
	./runfrege examples.IExpr
