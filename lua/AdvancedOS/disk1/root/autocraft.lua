-- allows crafting items by using the autocrafting component

local craftingGraph = {}

---@class graphEntry
---@field result string
---@field resultCount number
---@field ingredients table<string,number> -- itemid, count

-- key is the same as value.result
---@type table<string, graphEntry>
craftingGraph.graph = {}

craftingGraph.graph["minecraft:ladder"] = {
    ingredients = {["minecraft:stick"] = 7},
    result = "minecraft:ladder",
    resultCount = 3
}

craftingGraph.graph["minecraft:stick"] = {
    ingredients = {["minecraft:oak_planks"] = 2},
    result = "minecraft:stick",
    resultCount = 4
}

craftingGraph.graph["minecraft:oak_planks"] = {
    ingredients = {["minecraft:oak"] = 1},
    result = "minecraft:oak_planks",
    resultCount = 4
}

---@class craftingPlanEntry
---@field recipe graphEntry
---@field craftCount number


local stack = {}
function stack.new()
    return setmetatable({backing={}}, {__index=stack})
end

function stack:push(item)
    table.insert(self.backing, item)
end

function stack:pop()
    return table.remove(self.backing, #self.backing)
end

function stack:isEmpty()
    return #self.backing == 0
end


local function reserveItemsForCraft(itemId, count)
    return itemId == "minecraft:oak"
end

local function getRecipeForItem(itemId)
    return craftingGraph.graph[itemId]
end

---autocraft the given item amount
---@param itemId string
---@param count number
local function craftItems(itemId, count)
    assert(count > 0)
    local itemsToCraft = stack.new()
    itemsToCraft:push({itemId, count})

    ---@type craftingPlanEntry[]
    local craftingPlan = {}
    repeat
        --print("Need to craft:")
        --for k,v in ipairs(itemsToCraft.backing) do
        --  print(table.unpack(v))
        --end
    
        -- check if we have enough of this ingredient available already. Reserve as much as we need, and if theres anything left, autocraft the rest.
        local itemToCraft, amountToCraft = table.unpack(itemsToCraft:pop())
        if itemId == itemToCraft or not reserveItemsForCraft(itemToCraft, amountToCraft) then
            -- we need to actually craft this item            
            -- find possible recipes; if theres exactly one, use that, if theres none then we simply are missing this item as an ingredient
            -- if we find a recipe, add the ingredients to the stack, and store in craftingPlan which recipe we should craft and how often
            local recipe = getRecipeForItem(itemToCraft)
            if not recipe then
                error("We dont have "..amountToCraft.." "..itemToCraft.. " in stock and we also dont know of a recipe that allows us to craft it. "..
                    "Therefore we cannot autocraft the requested item ("..itemId..").")
            end
            local recipeNeedsToBeAppliedThisManyTimes = math.ceil(amountToCraft/recipe.resultCount)

            ---@type craftingPlanEntry
            local planItem = {
                recipe = recipe,
                craftCount = recipeNeedsToBeAppliedThisManyTimes
            }
            table.insert(craftingPlan, planItem)
            
            -- could be worth tracking leftover products, though these may only become available after we have planned out the crafting of the current item, to avoid loops sortof
            for key, value in pairs(recipe.ingredients) do
                itemsToCraft:push({key, value * recipeNeedsToBeAppliedThisManyTimes})
            end
        else
        -- else we have reserved the items via reserveItemsForCraft
        end
    until itemsToCraft:isEmpty();

    for i=#craftingPlan, 1, -1 do
        local value = craftingPlan[i]
        print("Crafting "..value.craftCount.."x"..value.recipe.resultCount.."="..(value.craftCount*value.recipe.resultCount).." "..value.recipe.result)
    end
end


craftItems("minecraft:ladder", 128)