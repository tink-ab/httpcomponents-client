java_library(
    name = "tink_httpclient",
    srcs = glob(["httpclient/src/main/**/*.java"]),
    visibility = ["//visibility:public"],
    javacopts = ['-XepDisableAllChecks'],
    deps = [
	   "@se_tink_httpcore//:tink_httpcore",
	   "@commons_logging_commons_logging//jar",
	   "@commons_codec_commons_codec//jar",
    ],
)
