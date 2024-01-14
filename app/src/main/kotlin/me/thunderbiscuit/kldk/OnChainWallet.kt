package me.thunderbiscuit.kldk

import me.thunderbiscuit.kldk.utils.Config
import me.thunderbiscuit.kldk.utils.toHex
import org.bitcoindevkit.*
import org.bitcoindevkit.Wallet

object OnChainWallet {
    private lateinit var wallet: Wallet
    private lateinit var blockchain: Blockchain

    init {
        val electrumURL: String = Config.electrumUrl
        val blockchainConfig = BlockchainConfig.Electrum(ElectrumConfig(electrumURL, null, 10u, 20u, 10u, true))
        blockchain = Blockchain(blockchainConfig)
        val bip32RootKey = DescriptorSecretKey(
            network = Network.TESTNET,
            mnemonic = Mnemonic.fromString(Config.mnemonic),
            password = ""
        )
        wallet = Wallet(
            descriptor = createExternalDescriptor(bip32RootKey, Network.TESTNET),
            changeDescriptor = createInternalDescriptor(bip32RootKey, Network.TESTNET),
            network = Network.TESTNET,
            databaseConfig = DatabaseConfig.Sqlite(SqliteDbConfiguration("bdk-onchain-sqlite")),
        )
    }

    object LogProgress: Progress {
        override fun update(progress: Float, message: String?) {
            println("Sync onchain wallet")
        }
    }

    private fun createExternalDescriptor(rootKey: DescriptorSecretKey, network: Network): Descriptor {
        val externalPath: DerivationPath = DerivationPath("m/84h/1h/0h/0")
        return Descriptor("wpkh(${rootKey.extend(externalPath).asString()})", network)
    }

    private fun createInternalDescriptor(rootKey: DescriptorSecretKey, network: Network): Descriptor {
        val externalPath: DerivationPath = DerivationPath("m/84h/1h/0h/1")
        return Descriptor("wpkh(${rootKey.extend(externalPath).asString()})", network)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun buildFundingTx(value: Long, script: ByteArray): ByteArray {
        wallet.sync(blockchain, LogProgress)
        val scriptListUByte: List<UByte> = script.toUByteArray().asList()
        val outputScript = Script(scriptListUByte)
        val (psbt, txDetails) = TxBuilder()
            .addRecipient(outputScript, value.toULong())
            .feeRate(4.0F)
            .finish(wallet)
        wallet.sign(psbt, SignOptions(
            trustWitnessUtxo = false,
            assumeHeight = null,
            allowAllSighashes = false,
            removePartialSigs = true,
            tryFinalize = true,
            signWithTapInternalKey = true,
            allowGrinding = true
        ))

        val rawTx = psbt.extractTx().serialize().toUByteArray().toByteArray()
        println("The raw funding tx is ${rawTx.toHex()}")
        return rawTx
    }
}
