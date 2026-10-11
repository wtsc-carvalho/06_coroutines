// ==========================================
// Prática 1: Sua Primeira Coroutine (Exercícios)
// ==========================================
import kotlinx.coroutines.*

fun executaPratica1() = runBlocking {
    println("\n--- Prática 1 ---")
    println("Início - Thread: ${Thread.currentThread().name}")

    val tarefa1 = launch {
        delay(2000)
        println("Tarefa 1 finalizada após 2 segundos")
    }

    // Exercício 1: Segunda coroutine
    val tarefa2 = launch {
        delay(1000)
        println("Tarefa 2 finalizada após 1 segundo")
    }

    // Exercício 2: async e await
    val tarefa3 = async {
        delay(500)
        42
    }
    
    println("Continuando enquanto as coroutines estão em execução...")
    
    val resultado = tarefa3.await()
    println("Resultado da tarefa 3: $resultado")
    
    // Exercício 3: Se comentarmos os joins abaixo, o programa pode encerrar
    // antes de tarefa1 ou tarefa2 terminarem de rodar. O runBlocking por padrão
    // espera suas filhas, mas em cenários reais de thread comum isso importa muito.
    tarefa1.join()
    tarefa2.join()
    println("Fim")
}


// ==========================================
// Prática 2: Dispatchers (Exercícios)
// ==========================================

// Exercício 1
suspend fun salvarNoArquivo(conteudo: String) {
    withContext(Dispatchers.IO) {
        println("Salvando arquivo na Thread: ${Thread.currentThread().name}")
        delay(800)
        println("Arquivo salvo: $conteudo")
    }
}

// Exercício 2
suspend fun calcularPrimos(limite: Int): List<Int> {
    return withContext(Dispatchers.Default) {
        println("Calculando primos na Thread: ${Thread.currentThread().name}")
        val primos = mutableListOf<Int>()
        for (num in 2..limite) {
            var ehPrimo = true
            for (i in 2 until num) {
                if (num % i == 0) {
                    ehPrimo = false
                    break
                }
            }
            if (ehPrimo) primos.add(num)
        }
        primos
    }
}

suspend fun operacaoDeRede(): String {
    return withContext(Dispatchers.IO) {
        delay(1000)
        "Dados recebidos da rede!"
    }
}

suspend fun processarDados(dados: String): String {
    return withContext(Dispatchers.Default) {
        delay(500)
        dados.uppercase()
    }
}

fun executaPratica2() = runBlocking {
    println("\n--- Prática 2 ---")
    val tempoInicio = System.currentTimeMillis()

    // Exercício 3: Execução paralela (simulação)
    val deferredRede = async { operacaoDeRede() }
    val deferredPrimos = async { calcularPrimos(100) }
    
    // Esperamos as duas juntas
    val dados = deferredRede.await()
    val primos = deferredPrimos.await()
    
    val resultadoProc = processarDados(dados)
    salvarNoArquivo(resultadoProc)
    
    println("Primos calculados: $primos")
    val tempoFim = System.currentTimeMillis()
    println("Tempo total Prática 2: ${tempoFim - tempoInicio} ms")
}


// ==========================================
// Prática 3: Coroutines no ViewModel (Exercícios)
// ==========================================
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

data class Clima(val cidade: String, val temperatura: Int, val condicao: String, val umidade: Int)

class ClimaRepository {
    suspend fun buscarClima(cidade: String): Clima {
        delay(2000)
        return when (cidade.lowercase()) {
            "são paulo" -> Clima("São Paulo", 25, "Parcialmente nublado", 78)
            "rio de janeiro" -> Clima("Rio de Janeiro", 32, "Ensolarado", 65)
            "curitiba" -> Clima("Curitiba", 18, "Chuvoso", 90)
            else -> Clima(cidade, 22, "Tempo agradável", 70)
        }
    }
}

sealed interface ClimaUiState {
    data object Idle : ClimaUiState
    data object Carregando : ClimaUiState
    data class Sucesso(val clima: Clima, val historico: List<String>) : ClimaUiState // Exercício 1
    data class Erro(val mensagem: String) : ClimaUiState
}

class ClimaViewModel(private val repository: ClimaRepository = ClimaRepository()) : ViewModel() {
    private val _uiState = MutableStateFlow<ClimaUiState>(ClimaUiState.Idle)
    val uiState: StateFlow<ClimaUiState> = _uiState.asStateFlow()
    
    private val historicoCidades = mutableListOf<String>()

    fun buscarClima(cidade: String) {
        if (cidade.isBlank()) return
        
        // Exercício 3: viewModelScope garante que a coroutine morre com a tela
        viewModelScope.launch {
            _uiState.value = ClimaUiState.Carregando
            try {
                val clima = repository.buscarClima(cidade)
                if (!historicoCidades.contains(clima.cidade)) {
                    historicoCidades.add(0, clima.cidade)
                }
                _uiState.value = ClimaUiState.Sucesso(clima, historicoCidades.toList())
            } catch (e: Exception) {
                _uiState.value = ClimaUiState.Erro("Não foi possível buscar: ${e.message}")
            }
        }
    }
}

@Composable
fun ClimaScreen(viewModel: ClimaViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var cidade by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("🌤 Previsão do Tempo", style = MaterialTheme.typography.headlineMedium)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = cidade,
                onValueChange = { cidade = it },
                label = { Text("Cidade") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { viewModel.buscarClima(cidade) }) { Text("Buscar") }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (val state = uiState) {
                is ClimaUiState.Idle -> Text("Digite uma cidade para ver o clima.", color = MaterialTheme.colorScheme.outline)
                is ClimaUiState.Carregando -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Buscando clima...")
                    }
                }
                is ClimaUiState.Sucesso -> {
                    Column {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(state.clima.cidade, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("${state.clima.temperatura}°C", fontSize = 64.sp, fontWeight = FontWeight.Light)
                                Text(state.clima.condicao, color = MaterialTheme.colorScheme.primary)
                                Text("Umidade: ${state.clima.umidade}%", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        Text("Histórico:", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            state.historico.take(3).forEach { histCidade ->
                                AssistChip(onClick = { 
                                    cidade = histCidade
                                    viewModel.buscarClima(histCidade) 
                                }, label = { Text(histCidade) })
                            }
                        }
                    }
                }
                is ClimaUiState.Erro -> Text(state.mensagem, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}


// ==========================================
// Prática 4: Cancelamento de Coroutines (Exercícios)
// ==========================================

fun executaPratica4() = runBlocking {
    println("\n--- Prática 4 ---")
    println("Iniciando download...")

    val downloadJob = launch {
        try {
            repeat(10) { i ->
                if (!isActive) return@launch
                delay(500)
                println("Baixando parte ${i + 1}/10...")
            }
            println("Download completo!")
        } catch (e: CancellationException) {
            // Exercício 1: Tratando o CancellationException
            println("Erro/Aviso: Upload cancelado de forma cooperativa.")
        }
    }

    delay(1800)
    println("Usuário cancelou o download!")
    downloadJob.cancel()
    downloadJob.join()
    println("Prática 4 encerrada.")
}


// ==========================================
// Prática 5: Flow — Dados (Exercícios)
// ==========================================
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.collect

// Exercício 3: Map puro de Flow
fun fluxoQuadrados() = flow {
    listOf(1, 2, 3, 4, 5).forEach {
        delay(300)
        emit(it * it)
    }
}

class TemporizadorViewModel : ViewModel() {
    private val _segundos = MutableStateFlow(0)
    val segundos: StateFlow<Int> = _segundos.asStateFlow()

    private var job: Job? = null

    fun iniciar() {
        if(job?.isActive == true) return
        job = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _segundos.value++
            }
        }
    }

    fun pausar() { job?.cancel() }

    fun resetar() {
        job?.cancel()
        _segundos.value = 0
    }
}

@Composable
fun TemporizadorScreen(viewModel: TemporizadorViewModel = viewModel()) {
    val segundosTotais by viewModel.segundos.collectAsStateWithLifecycle()
    
    // Exercício 1: Formatação de Tempo (00:00)
    val minutos = segundosTotais / 60
    val segundosFormatados = segundosTotais % 60
    val displayTempo = String.format("%02d:%02d", minutos, segundosFormatados)

    Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Cronômetro", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text(displayTempo, fontSize = 72.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { viewModel.iniciar() }) { Text("Iniciar") }
            Button(onClick = { viewModel.pausar() }) { Text("Pausar") }
            Button(onClick = { viewModel.resetar() }) { Text("Reset") }
        }
    }
}

// Executando os testes de Console
fun main() {
    executaPratica1()
    executaPratica2()
    executaPratica4()
    
    runBlocking {
        println("\n--- Prática 5 (Map / Flow) ---")
        fluxoQuadrados().collect { quadrado ->
            println("Quadrado emitido: $quadrado")
        }
    }
}