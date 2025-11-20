# Máquina Virtual - Compiladores

Implementação de uma máquina virtual para execução de código objeto compilado.

## 📋 Índice

- [Como Executar](#-como-executar)
- [Como Testar](#-como-testar)
- [Arquivos de Teste](#-arquivos-de-teste)
- [Testes Automatizados](#-testes-automatizados)
- [Correções Implementadas](#-correções-implementadas)
- [Estrutura do Projeto](#-estrutura-do-projeto)

## 🚀 Como Executar

### Pré-requisitos

- Java 20 ou superior
- Maven (opcional, o projeto inclui wrapper)

### Executando a Aplicação

1. **Baixe/clone o projeto**

2. **Execute um dos comandos abaixo:**

   **No Git Bash:**
   ```bash
   ./mvnw javafx:run
   ```

   **No CMD/PowerShell (Windows):**
   ```cmd
   .\mvnw.cmd javafx:run
   ```

   **No Linux/Mac:**
   ```bash
   ./mvnw javafx:run
   ```

3. **Na interface gráfica:**
   - Clique no botão **"Abrir Arquivo"**
   - Selecione um arquivo `.obj` (ex: `gera4.obj`)
   - Clique no botão **"Executar"**
   - Veja a saída na área de texto inferior

## 🧪 Como Testar

### Teste Rápido (Recomendado para começar)

1. Execute a aplicação conforme instruções acima
2. Abra o arquivo `gera4.obj`
3. Clique em "Executar"
4. **Resultado esperado:** Deve aparecer `8` na saída

✅ **Se funcionou:** A VM está funcionando corretamente!

### Testes por Ordem de Complexidade

#### 1. **gera4.obj** ⭐ (Mais simples - NÃO precisa de entrada)
- **O que faz:** Adiciona 5 + 3
- **Saída esperada:** `8`
- **Status:** ✅ Funciona perfeitamente sem entrada do usuário

#### 2. **gera6.obj** (Teste com CALL - PRECISA de entrada)
- **O que faz:** Função que recebe um valor, multiplica por 2 e subtrai 5
- **Saída esperada:** `(entrada * 2) - 5`
- **Exemplo:** Se inserir `10`, deve imprimir `15`

#### 3. **gera7.obj** (Teste completo - PRECISA de entrada)
- **O que faz:** Funções com ALLOC/DALLOC que somam dois valores
- **Saída esperada:** Soma de dois valores inseridos
- **Como testar:** Inserir 2 números e ver a soma

#### 4. **gera3.obj e ger2.obj** (Condicionais - PRECISAM de entrada)
- **O que fazem:** Testam se número é > 0 (gera3) ou < 10 (ger2)
- **Saída esperada:** `1` ou `0`
- **Como testar:** Inserir diferentes valores e verificar a saída

#### 5. **gera5.obj** (Teste condicional - PRECISA de entrada)
- **O que faz:** Teste de condicional if-else
- **Status:** ✅ Teste intermediário

#### 6. **gera.obj** (Mais complexo - PRECISA de entrada)
- **O que faz:** Teste completo com múltiplas funções, CALL, ALLOC/DALLOC
- **Status:** ✅ Teste mais completo e avançado

### 📝 Passo a Passo Detalhado: Testando gera.obj

O arquivo `gera.obj` é o teste mais complexo, envolvendo múltiplas funções aninhadas, chamadas recursivas e gerenciamento de memória. Siga estes passos:

#### **Método 1: Via Interface Gráfica (Recomendado)**

1. **Inicie a aplicação:**
   ```bash
   ./mvnw javafx:run
   ```
   (No Windows CMD: `.\mvnw.cmd javafx:run`)

2. **Na interface gráfica:**
   - Clique no botão **"Abrir Arquivo"**
   - Navegue até a pasta `Maquina_virtual/`
   - Selecione o arquivo **`gera.obj`**
   - Clique no botão **"Executar"**

3. **Durante a execução, você precisará fornecer 3 entradas:**
   
   **Entrada 1:** (Primeira janela de diálogo)
   - O programa pedirá um número inteiro
   - **Exemplo:** Digite `5` e clique OK
   - ⚠️ **Importante:** Este valor deve ser **menor que 10** para que o programa continue corretamente
   
   **Entrada 2:** (Segunda janela de diálogo)
   - O programa pedirá outro número inteiro
   - **Exemplo:** Digite `3` e clique OK
   
   **Entrada 3:** (Terceira janela de diálogo)
   - O programa pedirá mais um número inteiro
   - **Exemplo:** Digite `7` e clique OK

4. **Resultado esperado:**
   - O programa irá somar os dois últimos valores inseridos
   - **Saída esperada:** Se você inseriu `3` e `7`, a saída será `10`
   - A saída aparecerá na área de texto inferior da interface

#### **Método 2: Via Teste Automatizado**

Para verificar apenas se o arquivo carrega corretamente (sem executar completamente):

```bash
./mvnw test -Dtest=VirtualMachineTest#testGera_ComplexFullTest
```

⚠️ **Nota:** Este teste apenas valida a estrutura do arquivo, não executa completamente devido às entradas necessárias.

#### **Método 3: Via Linha de Comando (Limitado)**

O arquivo `gera.obj` requer entrada interativa, então não pode ser testado completamente via linha de comando sem interface gráfica. A instrução `RD` abre diálogos JavaFX que precisam de interação do usuário.

#### **Entendendo o que o programa faz:**

1. **Função principal (rótulo 1):** Chama a função 2
2. **Função 2:** Chama a função 3 e imprime um resultado intermediário
3. **Função 3:** Chama a função 5 e depois soma dois valores
4. **Função 5:** Lê um valor (Entrada 1), verifica se é < 10, e se sim, chama função 4
5. **Função 4:** Lê dois valores (Entrada 2 e Entrada 3) e retorna
6. **Resultado final:** Soma dos valores das Entradas 2 e 3

#### **Exemplo de Teste Completo:**

- **Entrada 1:** `5` (deve ser < 10)
- **Entrada 2:** `10`
- **Entrada 3:** `20`
- **Saída esperada:** `30` (soma de 10 + 20)

#### **Troubleshooting:**

- ❌ **Se aparecer erro sobre entrada cancelada:** Certifique-se de clicar OK em todas as janelas de diálogo
- ❌ **Se não aparecer saída:** Verifique o console para mensagens de erro
- ⚠️ **Se a primeira entrada for ≥ 10:** O programa não chamará a função 4, e a saída pode ser diferente

### ⚠️ Nota sobre Instrução RD

A instrução `RD` (Read) abre uma caixa de diálogo JavaFX para entrada do usuário. 
Arquivos que usam `RD` requerem entrada manual durante a execução.

## 📁 Arquivos de Teste

Todos os arquivos estão na pasta `Maquina_virtual/`:

| Arquivo | Descrição | Entrada Necessária? |
|---------|-----------|---------------------|
| `gera4.obj` | ✅ Teste simples: adição (5+3=8) | ❌ Não |
| `gera3.obj` | ✅ Teste de if-else (maior que zero) | ✅ Sim (RD) |
| `ger2.obj` | ✅ Teste de condicional (menor que 10) | ✅ Sim (RD) |
| `gera5.obj` | ✅ Teste de condicional | ✅ Sim (RD) |
| `gera6.obj` | ✅ Teste com CALL e ALLOC/DALLOC | ✅ Sim (RD) |
| `gera7.obj` | ✅ Teste completo com funções | ✅ Sim (RD) |
| `gera.obj` | ✅ Teste complexo completo | ✅ Sim (RD) |

## 🔬 Testes Automatizados

O projeto inclui testes JUnit automatizados para validar o funcionamento da VM.

### Executando os Testes

```bash
./mvnw test
```

### Testes Implementados

- ✅ `testSimpleAddition` - Teste de adição simples
- ✅ `testLoadAndStore` - Teste de carregar e armazenar
- ✅ `testGera4_SimpleAddition` - Teste completo do gera4.obj
- ✅ `testGera3_ConditionalIfElse` - Valida estrutura do gera3.obj
- ✅ `testGer2_ConditionalLessThan` - Valida estrutura do ger2.obj
- ✅ `testGera5_FunctionCall` - Valida estrutura do gera5.obj
- ✅ `testGera6_FunctionWithAllocDalloc` - Valida estrutura do gera6.obj
- ✅ `testGera7_FunctionSum` - Valida estrutura do gera7.obj
- ✅ `testGera_ComplexFullTest` - Valida estrutura do gera.obj
- ✅ `testAllObjFilesLoadCorrectly` - Verifica que todos os arquivos carregam
- ✅ `testMemoryManagement` - Verifica gerenciamento de memória

**Resultado esperado:**
```
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 🔧 Correções Implementadas

### Correções Realizadas:

- ✅ **INV** - Corrigido (faz pop, nega e push ao invés de modificar diretamente)
- ✅ **NEG** - Implementado (negação lógica: 0→1, diferente de 0→0)
- ✅ **JMP/CALL/JMPF** - Melhorados (aceitam rótulos e números como índice)
- ✅ **RETURN** - Corrigido (retorna corretamente após CALL)
- ✅ **ALLOC/DALLOC** - Corrigidos (salvam/restauram valores da memória na pilha)
- ✅ **Scanner** - Corrigido (fechamento automático com try-finally)
- ✅ **DEBUG** - Configurável (desativado por padrão para saída mais limpa)

### Instruções Suportadas:

A VM suporta todas as instruções padrão:
- **Controle:** `START`, `HLT`, `JMP`, `JMPF`
- **Pilha:** `LDC`, `LDV`, `STR`, `INV`, `NEG`
- **Aritméticas:** `ADD`, `SUB`, `MULT`, `DIVI`
- **Lógicas:** `AND`, `OR`
- **Comparação:** `CME`, `CMA`, `CEQ`, `CDIF`, `CMEQ`, `CMAQ`
- **I/O:** `RD`, `PRN`
- **Funções:** `CALL`, `RETURN`, `ALLOC`, `DALLOC`

## 📂 Estrutura do Projeto

```
Maquina_virtual/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── virtualMachine/
│   │   │   │   ├── VirtualMachine.java    # Implementação principal da VM
│   │   │   │   ├── LinhaVM.java           # Representação de uma linha
│   │   │   │   ├── Memoria.java           # Representação da memória
│   │   │   │   └── LinhaRotulo.java       # Representação de rótulo
│   │   │   └── com/example/virtualmachine/
│   │   │       ├── InterfaceVM.java       # Interface gráfica
│   │   │       └── InterfaceVMController.java
│   │   └── resources/
│   │       └── com/example/virtualmachine/
│   │           ├── vm.fxml                # Layout da interface
│   │           └── Style.css
│   └── test/
│       └── java/virtualMachine/
│           └── VirtualMachineTest.java    # Testes automatizados
├── gera*.obj                              # Arquivos de teste
├── pom.xml                                # Configuração Maven
├── mvnw / mvnw.cmd                        # Maven wrapper
└── README.md                              # Este arquivo
```

## 💡 Dicas

- **DEBUG:** Para ativar logs detalhados, altere `DEBUG = false` para `DEBUG = true` em `VirtualMachine.java`
- **Entrada do Usuário:** Arquivos com `RD` abrem janela de diálogo - isso é normal
- **Problemas:** Se algo não funcionar, verifique o console para mensagens de erro
- **Testes:** Comece sempre com `gera4.obj` para verificar se tudo está funcionando

## 📝 Licença

Este projeto é uma implementação educacional de uma máquina virtual.

---

**🎉 Pronto para usar! Execute `./mvnw javafx:run` e comece testando com `gera4.obj`!**
