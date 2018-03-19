pragma solidity ^ 0.4 .18;

/**
 * @title Ownable
 * @dev The Ownable contract has an owner address, and provides basic authorization control
 * functions, this simplifies the implementation of "user permissions".
 */
contract Ownable {

  address public owner;

  event OwnershipTransferred(address indexed previousOwner, address indexed newOwner);

  /**
   * @dev The Ownable constructor sets the original `owner` of the contract to the sender
   * account.
   */
  function Ownable() public {
    owner = msg.sender;
  }

  /**
   * @dev Throws if called by any account other than the owner.
   */
  modifier onlyOwner() {
    require(msg.sender == owner);
    _;
  }

  /**
   * @dev Allows the current owner to transfer control of the contract to a newOwner.
   * @param newOwner The address to transfer ownership to.
   */
  function transferOwnership(address newOwner) onlyOwner public {
    require(newOwner != address(0));
    OwnershipTransferred(owner, newOwner);
    owner = newOwner;
  }

}

contract ContractHolder is Ownable {

  struct User {
    int ClientScore;
    int FreelancerScore;
    int totalContracts;
  }

  address dataHolder = 0x5AA72B91805713fAed3672C759f305B751C0a8D3;

  address[] activeContracts;

  mapping(address => User) users;

  mapping(address => uint) contracts;

  modifier onlyRegisteredJobContract() {
    //only a registered job contract is allowed to call this function
    require(activeContracts[contracts[msg.sender]] == msg.sender);
    _;
  }

  function getActiveContracts() constant external returns(address[]) {
    return activeContracts;
  }

  /**
   * @notice create a new job contract
   * @param link location of information about the employment contract
   * @dev link can be a URL or an IPFS hash
   * */
  function createNewFreelanceContract(string link) public {
    //create a new contract
    address newContract = new EmploymentContract(link, msg.sender);
    activeContracts.push(newContract);
    contracts[newContract] = activeContracts.length - 1;
  }

  /**
   * @notice After the contract is fullfilled the client pays to the freelancer and gives a rating to the freelancer
   * @param _client address of the client
   * @param _rating the rating of the freelancer 
   */
  function contractFullfilled(address _client, int _rating) public onlyRegisteredJobContract {
    users[_client].totalContracts++;
  }

  /**
   * @notice If dispute happens, freelancer or client can call this function. Tokenholder tribunal is informed and a dispute is registered
   */
  function dispute() public onlyRegisteredJobContract {
    //register this dispute in the token holder tribunal
    TokenHolderTribunal(DataHolder(dataHolder).getTokenHolderTribunal()).createDispute(msg.sender);
  }

  /**
   * @notice get called by the job contract after the token holder tribunal made decision. The winner of the THT gets the money locked in the contract
   * @return The winner of the THT is returned
   * */
  function finalizeDispute() public onlyRegisteredJobContract returns(uint) {
    return TokenHolderTribunal(DataHolder(dataHolder).getTokenHolderTribunal()).FinalizeDispute(msg.sender);
  }

}

contract DataHolder is Ownable {

  address tokenHolderTribunal;

  address contractHolder;

  address tokenContract = 0x005c44cDA66E18c3732e57EE1A3c6F603DAD029E;

  /**
   *@notice returns address of THT
   *@return the THT
   */
  function getTokenHolderTribunal() external constant returns(address) {
    return tokenHolderTribunal;
  }

  /*
   *@notice returns address of contract holder
   *@return the contract holder
   */
  function getContractHolder() external constant returns(address) {
    return contractHolder;
  }

  /*
   *@notice setter for THT
   *@param tokenHolderTribunal address of THT to set
   */
  function setTokenHolderTribunal(address newTokenHolderTribunal) external onlyOwner {
    tokenHolderTribunal = newTokenHolderTribunal;
  }

  /*
   *@notice setter for contract holder
   *@param contractHolder the address of the contract holder
   */
  function setContractHolder(address newContractHolder) external onlyOwner {
    contractHolder = newContractHolder;
  }

  /*
   *@notice returns the address of the Lancer TokenHolderTribunal
   *@return the address of the Lancer Token
   */
  function getToken() external constant returns(address) {
    return tokenContract;
  }

  /**
   *@notice setter for the Lancer Token contract
   *@param tokenContract the address of the Lancer token
   */
  function setToken(address newTokenContract) external onlyOwner {
    tokenContract = newTokenContract;
  }
}

contract EmploymentContract is Ownable {

  struct Bid {
    uint price;
    address freelancer;
  }

  //link to the information depending the working
  /*todo: should be replaced with IPFS*/
  string public link;

  //address of the client(Creator of the contract) - owner

  /**
   *address to the  data holder -> contains the addresses to all important contractState
   *allows us to just replace a single contract with another one
   *after replacing everything works the same as before - except this contract communicate with a different contract
   */
  address dataHolder = 0x5AA72B91805713fAed3672C759f305B751C0a8D3;

  //the state of this job e.g. if its finished
  enum ContractState {
    SearchingFreelancer,
    FreelancerStartsWorking,
    TokenholderTribunal,
    CancelByFreelancer,
    FinishedSuccessful,
    ClientLostInTHT,
    FreelancerLostInTHT
  }

  //to store the current state
  ContractState contractState;

  //rating of the freelancer and client
  int public freelancerRating;
  uint clientRating;

  //bids of the freelancer
  Bid[] bid;

  //the bid the client selected
  Bid chosenBid;

  modifier onlyFreelancer() {
    //only freelancer are allowed to call this function
    require(msg.sender == chosenBid.freelancer);
    _;
  }

  /** @notice should be called by the contract holder contract
   *@param _link link to information of the contract (URL or IPFS hash)
   *@param _client address of the client
   */
  function EmploymentContract(string _link, address _client) public {
    link = _link;
    owner = _client;
    //set the current state of the contract to -> "In Search of a Freelancer"
    contractState = ContractState.SearchingFreelancer;
  }

  /**@notice returns the link to the Contract information
   *@return the link to the information of the contract
   *todo: should be replaced with IPFS
   */
  function getLink() public constant returns(string) {
    return link;
  }

  /**
   *@notice returns the client address
   *@return the client address
   */
  function getClient() public constant returns(address) {
    return owner;
  }

  /**
   *@dev returns the current state of the contract in readable form
   *@return the state of the contract as readable string
   */
  function getStateofContractString() external constant returns(string) {
    if (contractState == ContractState.SearchingFreelancer) {
      return "In Search of a Freelancer";
    } else if (contractState == ContractState.FreelancerStartsWorking) {
      return "Freelancer started working";
    } else if (contractState == ContractState.TokenholderTribunal) {
      return "Token Holder Tribunal is active";
    } else if (contractState == ContractState.CancelByFreelancer) {
      return "Freelancer cancelled the contract";
    } else if (contractState == ContractState.FinishedSuccessful) {
      return "Contract Successful finished";
    } else if (contractState == ContractState.ClientLostInTHT) {
      return "Client Lost in the Token Holder Tribunal";
    } else if (contractState == ContractState.FreelancerLostInTHT) {
      return "Freelancer Lost in the Token Holder Tribunal";
    } else {
      return "Error";
    }
  }

  /**
   *@notice get state of contract
   *@param the state of the contract as enum
   */
  function getStateofContract() external constant returns(ContractState) {
    return contractState;
  }

  /**
   *@notice returns the address of the selected freelancer (The one that works on this job)
   *@return address of the freelancer, who is working on the contract
   */
  function getChosenFreelancer() external constant returns(address) {
    return chosenBid.freelancer;
  }

  /**
   *@notice get the price of the selected freelancer
   *@return price of the freelancer, who is working on the contract
   */
  function getBalance() external constant returns(uint) {
    return chosenBid.price;
  }

  /**
   *@notice get the amount of bids for this job
   *@return how many bids are there for the contract
   */
  function getNumberOfBid() external constant returns(uint) {
    return bid.length;
  }

  /**
   *@notice add a bid yourself
   *@param _price how many money you'd like for doing the contract
   */
  function addBid(uint256 _price) external {
    Bid memory b;
    //convert the entered amount to wei
    b.price = _price * 10 ** 18;
    b.freelancer = msg.sender;
    bid.push(b);
  }

  /**
   *@notice get the price of a certain bid
   *@return price of a certain bid
   */
  function getBidPrice(uint index) external constant returns(uint) {
    if (bid.length > index)
      return bid[index].price;
    else
      return 0;
  }

  /**
   *@notice returns the freelancers address of a certain bid
   *@return address of freelancer of a certain bid
   */
  function getBidFreelancer(uint index) external constant returns(address) {
    if (bid.length > index)
      return bid[index].freelancer;
    else
      return 0;
  }

  /**
   *@notice The client selects a freelancer
   *@param the index of the selected freelancer
   */
  function create(uint index) payable external  onlyOwner {
    require(bid.length > index);
    require(msg.value == bid[index].price);

    chosenBid = bid[index];
    contractState = ContractState.FreelancerStartsWorking;
  }

  /**
   *@notice if the client accepts the work of the freelancer after that the contract is finished successfully
   *@param _freelancerRating the rating the client is giving to the freelancer
   */
  function payFreelancer(int _freelancerRating) external onlyOwner {

    //only if the state of the contract is FreelancerStartsWorking
    require(contractState == ContractState.FreelancerStartsWorking);

    //set the state of this job to finished
    contractState = ContractState.FinishedSuccessful;

    freelancerRating = _freelancerRating;

    //tell the  contract holder that this job is successfully finished
    ContractHolder(DataHolder(dataHolder).getContractHolder()).contractFullfilled(owner, freelancerRating);

    //send the money to the freelancer
    chosenBid.freelancer.transfer(this.balance);
  }

  /**
   *@notice If the contract is successfully finished, the freelacer is allowed to rate the client
   *@param _clientRating the rating of the client
   */
  function rateClient(uint _clientRating) external onlyFreelancer {
    require(contractState == ContractState.FinishedSuccessful);
    clientRating = _clientRating;
  }

  /**
   *@notice If there is a dispute every side is allowed to ask the token holder
   *@param reason A description, why the THT is called
   */
  function callTokenHolderTribunal(string reason) external onlyOwner onlyFreelancer {
    // only possible if the freelancer started working
    require(contractState == ContractState.FreelancerStartsWorking);

    //set the state of this job to "Token Holder Tribunal is active"
    contractState = ContractState.TokenholderTribunal;
    ContractHolder(DataHolder(dataHolder).getContractHolder()).dispute();
  }

  /**
   *@notice After the token holder made a decision, this function can be called to finish the dispute and send the money to the winning side
   */
  function finalizeDispute() public onlyOwner onlyFreelancer {

    //only the client and the freelancer are allowed to call this function
    uint result = 2;

    //retrieve the result from the token holder tribunal
    result = ContractHolder(DataHolder(dataHolder).getContractHolder()).finalizeDispute();

    //if the result is not 0 or 1 cancel
    require(result == 0 || result == 1);

    //if the token holder decided that the client war right
    if (result == 0) {
      owner.transfer(this.balance);

      contractState = ContractState.FreelancerLostInTHT;
    } else if (result == 1) {
      //if the token holder decided that the freelancer war right
      chosenBid.freelancer.transfer(this.balance);
      contractState = ContractState.ClientLostInTHT;
    }
  }

  /**
   *@notice If the freelancer volutarily chooses to stop working. The client will get the money back.
   */
  function stopWork() public onlyFreelancer {
    //only the freelancer is allowed to call this function
    contractState = ContractState.CancelByFreelancer;

    //send the money to the client
    require(owner.send(this.balance));
  }

}

/**
 *@title Interface for the Token
 */
contract Token {

  function balanceOf(address _owner) public constant returns(uint256);

  function getLastTransferred(address owner) public  constant returns(uint);

}

/**
 *@title Token Holder Tribunal (THT) Allows Token Holder to vote on disputes betweet freelancer and client
 */
contract TokenHolderTribunal {

  //holds information about a dispute
  struct dispute {
    mapping(uint => uint256) option;
    uint timeEnd;
    uint timeEndReveal;
    uint startTime;
    mapping(address => bytes32) secretVote;
    mapping(address => uint256) amount;
    uint arrayEntry;
  }

  mapping(address => dispute) disputes;

  address[] adisputes;

  address tokenContract = 0x005c44cDA66E18c3732e57EE1A3c6F603DAD029E;

  address dataHolder = 0xE323B02664548bc6410C3B93edfedaC22Aa3e423;

  function createDispute(address disputeContract) external {
    //create new dispute
    dispute memory disp;

    //set dispute start to current time + 1 day
    //can be changed by the token holder
    disp.timeEnd = block.timestamp + 1 * 1 days;

    //set the reveal period start time to current time + 2 days
    //can be changed by the token holder
    disp.timeEndReveal = block.timestamp + 2 * 1 days;

    //start time of the dispute
    disp.startTime = block.timestamp;

    //to later delete the dispute from the array
    disp.arrayEntry = adisputes.length;

    //save the dispute
    disputes[disputeContract] = disp;
    adisputes.push(disputeContract);
  }

  /**
   *@notice check how long you have before you can't vote for a certain dispute anymore
   *@param IDDispute the Id of the dispute
   */
  function howLongIsDisputeStillRunning(uint IDDispute) public constant returns(uint) {
    //if the voting period is over return 0
    if (disputes[adisputes[IDDispute]].timeEndReveal < block.timestamp) return 0;

    // otherwise return the time which remains before you can't vote anymore
    else return disputes[adisputes[IDDispute]].timeEnd - block.timestamp;
  }

  /**
   *@notice check how long you have before you can't reveal your vote for a certain dispute anymore
   *@param IDDispute The Id of the dispute
   */
  function howLongIsDisputeRevealStillRunning(uint IDDispute) public constant returns(uint) {
    //if the revealing period is over return 0
    if (disputes[adisputes[IDDispute]].timeEnd < block.timestamp) return 0;

    // otherwise return the time which remain before you can't reveal your vote anymore
    else return disputes[adisputes[IDDispute]].timeEndReveal - block.timestamp;
  }

  /**
   *@notice return all disputes at the moment
   *@return array of all employment contracts with disputes
   */
  function getDisputesAtTheMoment() public constant returns(address[]) {
    return adisputes;
  }

  /**
   *@notice Nobody is able to see the result before everyone voted. Has to be called before submitting to StoreSecretVote.
   *@param salt The salt
   *@param optionID Id of the dispute
   */
  function generateVoteSecret(string salt, uint optionId) pure external returns(bytes32) {
    //return SHA3 hash, has to be sent to StoreSecretVote
    return keccak256(salt, optionId);
  }

  /**
   *@notice send the generated hash in generateVoteSecret to this function
   *@param secret The hashed decision
   *@param IDDispute The Id of the dispute
   */
  function storeSecretVote(bytes32 secret, uint IDDispute) public {
    // if the voting period already ended -> throw
    require(now <= disputes[adisputes[IDDispute]].timeEnd);

    //save the hash
    disputes[adisputes[IDDispute]].secretVote[msg.sender] = secret;

    //save the amount of token the sender own at the moment
    //the sender isn't able to reveal the vote if he has less token than here
    disputes[adisputes[IDDispute]].amount[msg.sender] = Token(tokenContract).balanceOf(msg.sender);
  }

  /**
   *@dev for testing. Returns the hash the sender submitted in storeSecretVote
   *@param IDDispute id of the dispute
   *@return The hash of the secret vote
   */
  function returnSecretVoteHash(uint IDDispute) public constant returns(bytes32) {
    return disputes[adisputes[IDDispute]].secretVote[msg.sender];
  }

  /**
   * @notice Reveal the option you chose to vote for
   *@param salt The salt
   *@param optionID The decision. For whom you decided.
   *@param IDDispute Id of the dispute
   */
  function revealVote(string salt, uint optionID, uint IDDispute) public {
    //only possible after the voting time is over
    require(block.timestamp >= disputes[adisputes[IDDispute]].timeEnd);

    //only possible if the reveal time limit hasn't ended
    //You aren't able to reveal your vote afterwards
    require(block.timestamp <= disputes[adisputes[IDDispute]].timeEndReveal);

    //generate the hash to check if it is the same as submitted before
    bytes32 a = keccak256(salt, optionID);
    require(a == disputes[adisputes[IDDispute]].secretVote[msg.sender]);

    //don't allow to vote anything else than yes or no at the moment
    require(optionID <= 1);

    //check if the sender still has the same amount of token or more
    //if he has less token he isn't allowed to reveal his vote
    //this prevents double voting by just sending the token to another address and resubmit a vote
    //with this we don't have to disable transactions of token like the DAO
    require(disputes[adisputes[IDDispute]].amount[msg.sender] >= Token(tokenContract).balanceOf(msg.sender));

    //if he transferred token he isn't allowed to reveal his vote
    //this prevents someone from storing a vote then sending it to another wallet and storing another vote and so on
    //when the revealing period comes he could do the same thing and just sends the fund around and reveal
    //--------if(Token(TokenContract).getLastTransferred(msg.sender)>disputes[adisputes[IDDispute]].startTime) throw;

    //add the voting power(token in possesion) to the result
    //if the sender has more token than before the old amount is still used here
    disputes[adisputes[IDDispute]].option[optionID] += disputes[adisputes[IDDispute]].amount[msg.sender];

    //delete all entrys
    //the sender isn't able to reveal his vote again
    delete disputes[adisputes[IDDispute]].secretVote[msg.sender];
    delete disputes[adisputes[IDDispute]].amount[msg.sender];
  }

  /**
   *@dev for testing. Returns the amount of token the sender holds
   *@return The amount of tokens the sender holds
   */
  function fundsOf() constant external returns(uint256) {
    return Token(tokenContract).balanceOf(msg.sender);
  }

  /**
   *@notice finish the dispute and send the result to the other contracts
   *@param disputeContract The address of the contract, that is currently under dispute
   *@return the winner of the THT
   */
  function FinalizeDispute(address disputeContract) external returns(uint) {
    // only ContractHolder is able to call this function
    require(msg.sender == DataHolder(dataHolder).getContractHolder());

    //store the winner before deleting the dispute from the blockchain
    uint winner;

    //the voting option with the more supporter wins
    if (disputes[disputeContract].option[0] > disputes[disputeContract].option[1])
      winner = 0;
    else
      winner = 1;

    //delete the dispute
    delete adisputes[disputes[disputeContract].arrayEntry];
    delete disputes[disputeContract];


    //return the winner to the ContractHolder contract
    return winner;
  }

  /**
   *@notice returns the result of the vote
   *@param IDDispute The Id of the dispute
   *@param optionID The option you'd like to see
   *@param result of the vote
   */
  function getResult(uint IDDispute, uint optionID) public constant returns(uint) {
    return disputes[adisputes[IDDispute]].option[optionID];
  }

}
