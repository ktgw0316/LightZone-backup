##
# Automatic dependency generation makefile
#
# Paul J. Lucas [paul@lightcrafts.com]
##

AUTO_DEP_FLAGS:=	-MM -DAUTO_DEP $(DEFINES)

ifeq ($(PLATFORM),MacOSX)
  AUTO_DEP_FLAGS+=	-D_DARWIN_C_SOURCE -D__DARWIN_C_LEVEL=199506L
endif

ifeq ($(UNIVERSAL),1)
  ##
  # We need to use an architecture-specific INCLUDES, but since dependencies
  # are generated once regardless of the number of architectures, we have to
  # pick one, so we pick ARM.  Strictly speaking, this isn't the right thing do
  # do since it means the X86 compile will depend on ARM includes, but in
  # practice it's OK because this is only for dependency generation, not code
  # generation.
  ##
  AUTO_DEP_FLAGS+=	$(INCLUDES_ARM)
else
  AUTO_DEP_FLAGS+=	$(INCLUDES)
endif

ifeq ($(PROCESSOR),powerpc)
  ##
  # When doing dependency generation for Mac OS X universal binaries, -arch
  # parameters are not specified but -DLC_USE_ALTIVEC is.  This causes:
  #
  #	#error Use the "-maltivec" flag to enable PowerPC AltiVec support
  #
  # A way to get rid of this error is to specify -maltivec during dependency
  # generation for PowerPC only.
  ##
  AUTO_DEP_FLAGS+= 	-maltivec
endif

AUTO_DEP_CC:=		$(CC)

MAKEDEPEND:=		$(AUTO_DEP_CC) $(AUTO_DEP_FLAGS)

# Must not use := here!
ifeq ($(CYGWIN),1)
  define MAKE_DEP
    $(MAKEDEPEND) $1 | sed "s!^\([^ :]*\):!\1 $2 : !" | sed 's/\(\w\):/\/cygdrive\/\L\1/g' > $2; [ -s $2 ]  || $(RM) $2
  endef
endif

.%.d : %.c
	$(call MAKE_DEP,$<,$@)

.%.d : %.cpp
	$(call MAKE_DEP,$<,$@)

.%.d : %.m
	$(call MAKE_DEP,$<,$@)

.%.d : %.mm
	$(call MAKE_DEP,$<,$@)

##
# Include the dependency files only if the goals don't contain the word
# "clean".
##
ifneq ($(findstring clean,$(MAKECMDGOALS)),clean)
  -include $(OBJECTS:%.o=.%.d)
endif

# vim:set noet sw=8 ts=8:
