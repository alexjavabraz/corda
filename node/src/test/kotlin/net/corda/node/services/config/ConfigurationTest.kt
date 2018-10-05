package net.corda.node.services.config

import net.corda.core.internal.inputStream
import net.corda.core.internal.toPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

// TODO sollecitom add typed-property-based equivalent tests.
// TODO sollecitom add more tests, including writing to various formats / media, separate the tests in terms of reading, writing and using.
class ConfigurationTest {

    private object AddressesSpec : Configuration.Specification.Mutable() {

        val main by required<String>(description = "Externally visible address for RPC.")
        val admin by optional<String?>(default = null, description = "Admin address for RPC, mandatory when `useSsl` is set to `true`.")
    }

    private object RpcSettingsSpec : Configuration.Specification.Mutable() {

        val addresses by required<Configuration>(AddressesSpec, description = "Address configuration for RPC.")

        val useSsl by optional(default = false, description = "Whether to use SSL for RPC client-server communication")
    }

    private object NodeConfigSpec : Configuration.Specification.Mutable() {

        val myLegalName by required<String>(description = "Legal name of the identity of the Corda node")

        // TODO sollecitom rename the ones with config to start with `nested` or similar, otherwise is confusing.
        val rpcSettings by optional<Configuration>(RpcSettingsSpec, Configuration.empty(RpcSettingsSpec), description = "RPC settings")
    }

    @Test
    fun loading_from_specific_file_works() {

        val configFilePath = ConfigurationTest::class.java.getResource("node.conf").toPath()

        val configuration = Configuration.from.hocon.file(configFilePath).build(NodeConfigSpec)

        assertThat(configuration[NodeConfigSpec.myLegalName]).isEqualTo("O=Bank A,L=London,C=GB")

        val rpcSettings: Configuration = configuration[NodeConfigSpec.rpcSettings]

        val useSslForRpc = rpcSettings[RpcSettingsSpec.useSsl]

        assertThat(useSslForRpc).isEqualTo(false)

        val addresses: Configuration = rpcSettings[RpcSettingsSpec.addresses]

        assertThat(addresses[AddressesSpec.main]).isEqualTo("my-corda-node:10003")
        assertThat(addresses[AddressesSpec.admin]).isEqualTo("my-corda-node:10004")
    }

    @Test
    fun loading_from_resource_works() {

        val configuration = Configuration.from.hocon.resource("net/corda/node/services/config/node.conf").build(NodeConfigSpec)

        assertThat(configuration[NodeConfigSpec.myLegalName]).isEqualTo("O=Bank A,L=London,C=GB")

        val rpcSettings: Configuration = configuration[NodeConfigSpec.rpcSettings]

        val useSslForRpc = rpcSettings[RpcSettingsSpec.useSsl]

        assertThat(useSslForRpc).isEqualTo(false)

        val addresses: Configuration = rpcSettings[RpcSettingsSpec.addresses]

        assertThat(addresses[AddressesSpec.main]).isEqualTo("my-corda-node:10003")
        assertThat(addresses[AddressesSpec.admin]).isEqualTo("my-corda-node:10004")
    }

    @Test
    fun loading_from_input_stream_works() {

        val configuration = ConfigurationTest::class.java.getResource("node.conf").toPath().inputStream().use {

            Configuration.from.hocon.inputStream(it).build(NodeConfigSpec)
        }

        assertThat(configuration[NodeConfigSpec.myLegalName]).isEqualTo("O=Bank A,L=London,C=GB")

        val rpcSettings: Configuration = configuration[NodeConfigSpec.rpcSettings]

        val useSslForRpc = rpcSettings[RpcSettingsSpec.useSsl]

        assertThat(useSslForRpc).isEqualTo(false)

        val addresses: Configuration = rpcSettings[RpcSettingsSpec.addresses]

        assertThat(addresses[AddressesSpec.main]).isEqualTo("my-corda-node:10003")
        assertThat(addresses[AddressesSpec.admin]).isEqualTo("my-corda-node:10004")
    }

    @Test
    fun loading_from_string_works() {

        val rawConfig = "\"myLegalName\" = \"O=Bank A,L=London,C=GB\"\n\"rpcSettings\" = {\n\"useSsl\" = false\n\"addresses\" = {\n\"main\" = \"my-corda-node:10003\"\n\"admin\" = \"my-corda-node:10004\"}}"
        val configuration = Configuration.from.hocon.string(rawConfig).build(NodeConfigSpec)

        assertThat(configuration[NodeConfigSpec.myLegalName]).isEqualTo("O=Bank A,L=London,C=GB")

        val rpcSettings: Configuration = configuration[NodeConfigSpec.rpcSettings]

        val useSslForRpc = rpcSettings[RpcSettingsSpec.useSsl]

        assertThat(useSslForRpc).isEqualTo(false)

        val addresses: Configuration = rpcSettings[RpcSettingsSpec.addresses]

        assertThat(addresses[AddressesSpec.main]).isEqualTo("my-corda-node:10003")
        assertThat(addresses[AddressesSpec.admin]).isEqualTo("my-corda-node:10004")
    }

    @Test
    fun loading_from_system_properties_works() {

        System.setProperty("corda.configuration.myLegalName", "O=Bank A,L=London,C=GB")
        System.setProperty("corda.configuration.rpcSettings.useSsl", "false")
        System.setProperty("corda.configuration.rpcSettings.addresses.main", "my-corda-node:10003")
        System.setProperty("corda.configuration.rpcSettings.addresses.admin", "my-corda-node:10004")

        val configuration = Configuration.from.systemProperties("corda.configuration").build(NodeConfigSpec)

        assertThat(configuration[NodeConfigSpec.myLegalName]).isEqualTo("O=Bank A,L=London,C=GB")

        val rpcSettings: Configuration = configuration[NodeConfigSpec.rpcSettings]

        val useSslForRpc = rpcSettings[RpcSettingsSpec.useSsl]

        assertThat(useSslForRpc).isEqualTo(false)

        val addresses: Configuration = rpcSettings[RpcSettingsSpec.addresses]

        assertThat(addresses[AddressesSpec.main]).isEqualTo("my-corda-node:10003")
        assertThat(addresses[AddressesSpec.admin]).isEqualTo("my-corda-node:10004")
    }

    @Test
    fun cascade_order_is_respected() {

        System.setProperty("corda.configuration.myLegalName", "O=Bank B,L=London,C=GB")

        val configuration = Configuration.from.hocon.resource("net/corda/node/services/config/node.conf").from.systemProperties("corda.configuration").build(NodeConfigSpec)

        assertThat(configuration[NodeConfigSpec.myLegalName]).isEqualTo("O=Bank B,L=London,C=GB")

        val rpcSettings: Configuration = configuration[NodeConfigSpec.rpcSettings]

        val useSslForRpc = rpcSettings[RpcSettingsSpec.useSsl]

        assertThat(useSslForRpc).isEqualTo(false)

        val addresses: Configuration = rpcSettings[RpcSettingsSpec.addresses]

        assertThat(addresses[AddressesSpec.main]).isEqualTo("my-corda-node:10003")
        assertThat(addresses[AddressesSpec.admin]).isEqualTo("my-corda-node:10004")
    }

    @Test
    fun mutable_configuration_contract() {

        val legalName1 = "O=Bank A,L=London,C=GB"
        val configuration1 = Configuration.from.hocon.resource("net/corda/node/services/config/node.conf").build(NodeConfigSpec)

        val myLegalNameRetrieved1: String = configuration1[NodeConfigSpec.myLegalName]
        assertThat(myLegalNameRetrieved1).isEqualTo(legalName1)

        val legalName2 = "O=Bank B,L=London,C=GB"
        val configuration2 = configuration1.mutable()

        val myLegalNameRetrieved2 = configuration2[NodeConfigSpec.myLegalName]
        assertThat(myLegalNameRetrieved2).isEqualTo(legalName1)

        configuration2[NodeConfigSpec.myLegalName] = legalName2

        val myLegalNameRetrieved3: String = configuration2[NodeConfigSpec.myLegalName]
        assertThat(myLegalNameRetrieved3).isEqualTo(legalName2)

        val myLegalNameRetrieved4: String = configuration1[NodeConfigSpec.myLegalName]
        assertThat(myLegalNameRetrieved4).isEqualTo(legalName1)
    }

    @Test
    fun mutable_configuration_contract_with_configuration_properties() {

        val configuration1 = Configuration.from.hocon.resource("net/corda/node/services/config/node.conf").build(NodeConfigSpec)

        val newRpcSettings = Configuration.from.hocon.string("\"useSsl\" = false\n\"addresses\" = {\n\"main\" = \"my-corda-node:10003\"\n\"admin\" = \"my-corda-node:10004\"}").build(RpcSettingsSpec)

        val configuration2 = configuration1.mutable()

        val originalRpcSettings = configuration1[NodeConfigSpec.rpcSettings]

        assertThat(configuration2[NodeConfigSpec.rpcSettings]).isEqualToComparingFieldByField(originalRpcSettings)

        configuration2[NodeConfigSpec.rpcSettings] = newRpcSettings

        val overriddenRpcSettings = configuration2[NodeConfigSpec.rpcSettings]
        assertThat(overriddenRpcSettings).isEqualToComparingFieldByField(newRpcSettings)

        assertThat(configuration1[NodeConfigSpec.rpcSettings]).isEqualToComparingFieldByField(originalRpcSettings)
    }
}