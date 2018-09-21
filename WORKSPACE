load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")

git_repository(
    name = "tink_httpcore_4_4_9",
    remote = "git@github.com:tink-ab/httpcomponents-core.git",
    commit = "0f72fa2c392fee8388d327cb3462cd10d675c2e2"
)

maven_jar(
    name = "commons_logging_commons_logging",
    artifact = "commons-logging:commons-logging:1.1.3",
    sha1 = "f6f66e966c70a83ffbdb6f17a0919eaf7c8aca7f",
)


maven_jar(
    name = "commons_codec_commons_codec",
    artifact = "commons-codec:commons-codec:1.6",
    sha1 = "b7f0fc8f61ecadeb3695f0b9464755eee44374d4",
)
