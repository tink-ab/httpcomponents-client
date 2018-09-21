java_library(
    name = "tink_httpclient_4_5_5",
    srcs = glob(["httpclient/src/main/**/*.java"]),
    visibility = ["//visibility:public"],
    javacopts = ['-XepDisableAllChecks'],
    deps = [
	   "@tink_httpcore_4_4_9//:tink_httpcore_4_4_9",
	   "@commons_logging_commons_logging//jar",
	   "@commons_codec_commons_codec//jar",
    ],
)
