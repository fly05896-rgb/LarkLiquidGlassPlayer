cmake_minimum_required(VERSION 3.22.1)
project("eg_native_engine" LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_compile_options(-O3 -ffast-math -fvisibility=hidden)

add_library(
    eg_native_engine
    SHARED
    eg_native_engine.cpp
)

find_library(log-lib log)

target_link_libraries(
    eg_native_engine
    ${log-lib}
)
