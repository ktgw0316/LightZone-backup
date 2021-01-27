cmake_minimum_required(VERSION 3.1)
project(dcraw_lz C)
add_executable(${PROJECT_NAME} ${PROJECT_NAME}.c)

target_compile_definitions(${PROJECT_NAME} PRIVATE
	NODEPS LIGHTZONE
	$<$<PLATFORM_ID:Windows>: DJGPP>
	$<$<PLATFORM_ID:Linux Darwin>: fgetc=getc_unlocked>
	)
target_compile_options(${PROJECT_NAME} PRIVATE
	-Wno-unused-result -fopenmp
	$<$<PLATFORM_ID:Windows>: -static>
	$<$<PLATFORM_ID:Darwin>: -Xpreprocessor>
	)
target_link_libraries(${PROJECT_NAME}
	$<$<PLATFORM_ID:Windows>: mingwex ws2_32>
	$<$<PLATFORM_ID:Linux>: m>
	$<$<PLATFORM_ID:macosx>: omp>
	)

# install(TARGETS ${PROJECT_NAME} ../../../products)
