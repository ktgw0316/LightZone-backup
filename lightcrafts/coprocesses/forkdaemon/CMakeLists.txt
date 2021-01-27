cmake_minimum_required(VERSION 3.1)
project(LightZone-forkd CXX)
add_executable(${PROJECT_NAME} forkdaemon.cpp)

target_compile_definitions(${PROJECT_NAME} PRIVATE
	$<$<PLATFORM_ID:Linux Darwin>: USE_FORKER>
	)

# install(TARGETS ${PROJECT_NAME} ../../../products)
