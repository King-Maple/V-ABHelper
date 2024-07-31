
# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.
cmake_minimum_required(VERSION 3.18.1)

project("kernelsu")


set(protobuf_source_dir "protobuf-3.7.1")

set(libprotobuf_lite_files
		${protobuf_source_dir}/src/google/protobuf/arena.cc
		${protobuf_source_dir}/src/google/protobuf/extension_set.cc
		${protobuf_source_dir}/src/google/protobuf/generated_message_table_driven_lite.cc
		${protobuf_source_dir}/src/google/protobuf/generated_message_util.cc
		${protobuf_source_dir}/src/google/protobuf/implicit_weak_message.cc
		${protobuf_source_dir}/src/google/protobuf/parse_context.cc
		${protobuf_source_dir}/src/google/protobuf/io/coded_stream.cc
		${protobuf_source_dir}/src/google/protobuf/io/strtod.cc
		${protobuf_source_dir}/src/google/protobuf/io/zero_copy_stream.cc
		${protobuf_source_dir}/src/google/protobuf/io/zero_copy_stream_impl_lite.cc
		${protobuf_source_dir}/src/google/protobuf/message_lite.cc
		${protobuf_source_dir}/src/google/protobuf/repeated_field.cc
		${protobuf_source_dir}/src/google/protobuf/stubs/bytestream.cc
		${protobuf_source_dir}/src/google/protobuf/stubs/common.cc
		${protobuf_source_dir}/src/google/protobuf/stubs/int128.cc
		${protobuf_source_dir}/src/google/protobuf/stubs/io_win32.cc
		${protobuf_source_dir}/src/google/protobuf/stubs/status.cc
		${protobuf_source_dir}/src/google/protobuf/stubs/statusor.cc
		${protobuf_source_dir}/src/google/protobuf/stubs/stringpiece.cc
		${protobuf_source_dir}/src/google/protobuf/stubs/stringprintf.cc
		${protobuf_source_dir}/src/google/protobuf/stubs/structurally_valid.cc
		${protobuf_source_dir}/src/google/protobuf/stubs/strutil.cc
		${protobuf_source_dir}/src/google/protobuf/stubs/time.cc
		${protobuf_source_dir}/src/google/protobuf/wire_format_lite.cc
)

add_library(local_dobby STATIC IMPORTED)
set_target_properties(local_dobby PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/lib/${ANDROID_ABI}/libdobby.a)

include_directories(${protobuf_source_dir}/src
		include
		proto
)

add_library(kernelsu
        SHARED
        jni.cc
        ksu.cc

		utils/elf_util.cc
		utils/lsplt.cc
		utils/inject.cpp
		utils/ptrace_utils.cpp
)


add_library(inject
		SHARED

		pass.cpp

		${libprotobuf_lite_files}

		proto/update_metadata.pb.cc

		utils/elf_util.cc
		utils/lsplt.cc
		utils/inject.cpp
		utils/ptrace_utils.cpp
)

find_library(log-lib log)

target_link_libraries(kernelsu ${log-lib})
target_link_libraries(inject ${log-lib} local_dobby)