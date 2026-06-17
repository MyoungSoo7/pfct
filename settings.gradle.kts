plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
rootProject.name = "pfct"

include("common")
include("bootstrap")
include("modules:ledger")
include("modules:investment")
include("modules:lending")
